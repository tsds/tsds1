/*
 * Copyright (c) 2010, Regents of the University of Colorado
 * 
 * All rights reserved. Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package lasp.tss.iosp;

import org.apache.log4j.Logger;
import org.jdom.Element;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;

import lasp.tss.TSSException;

/**
 * IOSP for a flat IEEE 64-bit little-endian float data.
 * Time is the slowest varying dimension.
 * Assumes only one variable in the file. 
 * But Structure and Sequence can have multiple scalars per time sample.
 * 
 * @author Doug Lindholm
 */
public class BinIOSP extends AbstractIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(BinIOSP.class);
    
    /**
     * Initialize the IOSP. Set the byte order.
     * Note, we do not read the entire dataset in as in most other cases.
     */
    protected void init() {
        //Determine the byte order of the binary data.
        String endian = getProperty("byteOrder");
        if (endian == null) endian = "big"; //default to big endian
        int bo = -1;
        //if ("big".equals(endian)) bo = RandomAccessFile.BIG_ENDIAN;
        if (endian.toLowerCase().startsWith("big")) bo = RandomAccessFile.BIG_ENDIAN;
        else if (endian.toLowerCase().startsWith("little")) bo = RandomAccessFile.LITTLE_ENDIAN;
        else _logger.warn("Unable to parse byteOrder: " + endian + ". Defaulting to big-endian.");
        
        getFile().order(bo);
    }
    

    /**
     * Get number of time samples based on the file size.
     * Assume 8 bytes for each varaible.
     * Note, bin format is designed for a single variable.
     * Could be Scalar, Structure, or Sequence.
     */
    protected int getLength() {
        //See if it's already defined
        int length = super.getLength();
        if (length >= 0) return length; // -1 if undefined
        
        //Get length from size of data file
        RandomAccessFile raFile = getFile();
        Element ncel = getNetcdfElement();
        int nvar = 1;
        int n2 = 1;
        
        //Look for Structure or Sequence, assumes no more than one
        Element gel = ncel.getChild("group", ncel.getNamespace());
        if (gel != null) {
            //get number of variables in this group
            //don't count independent variables, i.e. name = shape
            List<Element> vars = gel.getChildren("variable", ncel.getNamespace());
            nvar = 0;
            for (Element var : vars) {
                String name = var.getAttributeValue("name");
                String shape = var.getAttributeValue("shape");
                if (name.equals(shape)) continue; //don't count independent (coordinate) variable
                nvar++;
            }
            if (nvar == 0) _logger.warn("No variables were found in the group: " + gel.getAttributeValue("name"));
            
            Element dim = gel.getChild("dimension", gel.getNamespace());
            if (dim != null) { //Sequence
                //Get the length of the Sequence dimension
                String len = dim.getAttributeValue("length");
                if (len != null) n2 = Integer.parseInt(len);
                else {
                    String msg = "No length defined for the Sequence dimension: " + dim.getName();
                    _logger.error(msg);
                    throw new TSSException(msg);
                }
            } //else Structure
        } //else Scalar
        
        try {
            length = ((int) raFile.length()) /nvar/n2/8;
        } catch (IOException e) {
            String msg = "Unable to get length from the size of the data file: " + raFile.getLocation();
            throw new TSSException(msg);
        }
        return length;
    }


    private Array readStructureComponent(Variable variable, Section section) {
        Array array = null;
        
        Group parent = variable.getParentGroup();
        List<Variable> vars = parent.getVariables();
        int nvar = vars.size();
        
        //Get index of this variable among siblings
        int ivar = vars.indexOf(variable);
        //todo: error if not found? shouldn't be possible
        
        //Get info on time selection (1st dimension). Should be no 2nd dim here
        int[] shape = section.getShape();
        int ntim = shape[0]; 
        int origin = section.getOrigin(0); 
        int stride = section.getStride(0); 
        int length = ntim; 
       
        int position = (origin*nvar + ivar) * 8;
        RandomAccessFile raFile = getFile();
        
        double[] data = null;
        try {
            raFile.seek(position); //skip to start
            data = new double[length];
            
            int i = 0; //index into data array
            for (int itim=0; itim<ntim; itim++) {
                raFile.readDouble(data, i, 1); //read 1 value into data array, one time sample
                i++;
                //skip nvar values for next read
                position += nvar * stride * 8; 
                raFile.seek(position);
            }
        } catch (IOException e) {
            String msg = "Failed reading from file: " + raFile.getLocation();
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        
        array = Array.factory(DataType.DOUBLE, shape, data);
        
        return array;
    }
    
    public Array readData(Variable variable, Section section) throws IOException, InvalidRangeException {
        Array array = null;

        //Need special handling of structure. 
        //Currently identifiable as a member of a group without an independent variable.
        Group parent = variable.getParentGroup();
        if (parent != null) {
            //Check if there is an independent variable (name = dimension).
            //If not, then this is a Structure, else Sequence.
            String[] dims = variable.getDimensionsString().split(" ");
            String dim = dims[dims.length - 1]; //last, fastest varying dimension
            Variable v = parent.findVariable(dim);

            if (v == null) {
                array = readStructureComponent(variable, section);
                return array; //return here so we don't interfere with a Sequence component
            }
        }

        //Get info on time selection (1st dimension).
        int[] shape = section.getShape();
        int ntim = shape[0]; 
        int origin = section.getOrigin(0); 
        int stride = section.getStride(0); 
        int length = ntim; //size of data array subset

        //Handle 2D section, if there is one. e.g. 2nd dim = Sequence domain
        int n2 = 1;
        int o2 = 0;
        //int s2 = 1; second dimension stride not supported, yet
        int n2orig = 1; //size of original 2nd dimension (i.e. number of wavelengths) in the backing array
        if (shape.length == 2) {
            n2 = shape[1];
            o2 = section.getOrigin(1);
            //s2 = section.getStride(1); 
            n2orig = variable.getShape(1);
            length *= n2;
        }

        RandomAccessFile raFile = getFile();
        int position = (origin * n2orig + o2) * 8; //bytes to skip to first time sample, assumes doubles
        raFile.seek(position); //skip to start of desired subset
        double[] data = new double[length];

        //read selectively if needed: for strides and/or 2nd dim subsetting
        if (stride > 1 || n2 != n2orig) {
            //read and skip for each time sample 
            int i = 0; //index into data array
            for (int itim=0; itim<ntim; itim++) {
                raFile.readDouble(data, i, n2); //read n2 values into data array, one time sample
                i += n2;
                //skip n values for next read
                position += n2orig * stride * 8;
                raFile.seek(position);
            }

        } else {
            //read in one lump
            raFile.readDouble(data, 0, length); 
        }

        array = Array.factory(DataType.DOUBLE, shape, data);

        return array;
    }


    public String getFileTypeDescription() {
        String s = "Flat binary file of doubles ordered same as backing array.";
        return s;
    }


    public String getFileTypeId() {
        return "TSS-Bin";
    }


}
