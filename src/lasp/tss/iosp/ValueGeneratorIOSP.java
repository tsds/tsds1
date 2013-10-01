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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.Variable;

/**
 * Return data as if each variable is an infinite series with a given "start" and "increment".
 * Similar to NcML's "values" element but generates the data values as needed
 * instead of creating and storing them all.
 * Requires that the length of the time dimension be defined in the NcML.
 * 
 * @author Doug Lindholm
 */
public class ValueGeneratorIOSP extends AbstractIOSP {
 
    private double _start = 0.;      //default: start at zero
    private double _increment = 1.;  //default: step by one   
    
    /**
     * Initialize the IOSP. Get the start and increment properties.
     */
    protected void init() {
        String start = getProperty("start");
        if (start != null) _start = Double.parseDouble(start);
        
        String increment = getProperty("increment");
        if (increment != null) _increment = Double.parseDouble(increment);
    }
    
    /**
     * Read the data for the given Section of the given Variable and return as an Array.
     */
    public Array readData(Variable var, Section section) throws IOException, InvalidRangeException {
        Array array = null;

        //deal with multiple variables in a Structure or Sequence (implemented using ncml groups)
        //Get this variable's index into the list of siblings.
        int ivar = 0;
        int nvar = 1;
        Group parent = var.getParentGroup();
        if (! parent.isRoot()) {
            List<Variable> vars = parent.getVariables();
            //count only dependent variables
            List<Variable> depvars = new ArrayList<Variable>();
            for (Variable v : vars) if (! v.isCoordinateVariable()) depvars.add(v);
            
            nvar = depvars.size();
            ivar = depvars.indexOf(var);
        }
        
        int[] shape = section.getShape();
        int ntim = shape[0]; 
        int origin = section.getOrigin(0); 
        int stride = section.getStride(0); 
        int length = ntim; //size of data array subset

        //Handle 2D section, if there is one. e.g. 2nd dim = Sequence domain
        int n2 = 1;
        int o2 = 0;
        int s2 = 1;
        int n2orig = 1; //size of original 2nd dimension (i.e. number of wavelengths) in the backing array
        if (shape.length == 2) {
            n2 = shape[1];
            o2 = section.getOrigin(1);
            s2 = section.getStride(1);
            n2orig = var.getShape(1);
            length *= n2;
        }
        
        double[] data = new double[length];
        int index = (origin * n2orig + o2) * nvar + ivar;
        int i=0;
        for (int itim=0; itim<ntim; itim++) {
            for (int i2=0; i2<n2; i2++) {
                data[i++] = getValue(index + i2 * s2 * nvar);
            }
            index += stride * n2orig * nvar;
        }
        
        array = Array.factory(DataType.DOUBLE, shape, data);
        
        return array;
    }

    /**
     * Return the generated data value for the given index.
     */
    private double getValue(int index) {
        double d = _start + _increment * index;
        return d;
    }
    
    public String getFileTypeDescription() {
        return "ValueGenerator";
    }

    public String getFileTypeId() {
        return "ValueGenerator";
    }

}
