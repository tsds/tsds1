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
package lasp.tss.writer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import lasp.tss.variable.SequenceVariable;
import lasp.tss.variable.TSSVariable;

/**
 * Write the dataset in the DAP specified binary format.
 * 
 * @author Doug Lindholm
 */
public class DataDdsWriter extends BinaryDataWriter {
    
    /**
     * The start of instance byte marker, XDR byte requires 3 extra bytes of padding
     */
    static protected byte[] START_OF_INSTANCE = new byte[] {0x5A, 0, 0, 0};

    /**
     * The end of sequence byte marker, XDR byte requires 3 extra bytes of padding
     */ 
    static protected byte[] END_OF_SEQUENCE = new byte[] {(byte) 0xA5, 0, 0, 0};

    /**
     * ByteBuffer just big enough to hold the data for one time sample, including DAP markers.
     */
    private ByteBuffer _byteBuffer;


    /**
     * Return String to be used in the "Content-Description" header.
     */
    public String getContentDescription() {
        return "dods-data";
    }
    
    /**
     * Initialize the writer. 
     */
    public void init() {
        //Compute the number of bytes needed for a time sample.
        int size = 4; //starts with a START_OF_INSTANCE marker for each time sample
        for (TSSVariable var : getVariables()) {
            size += var.getTimeSampleSizeInBytes();
            //add room for markers for Sequences, child variable size aleady counted
            if (var.isSequence()) {
                size += 4; //end marker
                int n = ((SequenceVariable) var).getLength(); //number of samples
                size += n * 4; //start of instance marker
            }
        }
        
        //Make a reusable ByteBuffer to contain a complete time sample.
        _byteBuffer = ByteBuffer.allocate(size);
        _byteBuffer.order(ByteOrder.BIG_ENDIAN); //DAP specifies Big Endian
    }
    
    /**
     * Write the header content.
     */
    public void writeHeader() {
        //print DDS
        String dds = getDataset().getDDS();
        writeBytes(dds+"\n"); 
        
        //print Data separator
        writeBytes("Data:\n");
    }

    /**
     * End the output with an END_OF_SEQUENCE marker.
     */
    public void writeFooter() {
        //end the TimeSeries Sequence
        writeBytes(END_OF_SEQUENCE);
    }

    /**
     * Write the data for the given time sample.
     */
    public void writeTimeSample(int timeIndex) {
        _byteBuffer.rewind(); //resuse for each time sample
        
        //Mark the start of each time sample
        putBytes(START_OF_INSTANCE);
        
        //For each projected variable in the TimeSeries
        for (TSSVariable var : getVariables()) {
            boolean valid = true;
            valid = loadTimeSample(var, timeIndex); //fill the ByteBuffer with data values
            if (! valid) {
                //don't write this time sample if any variable is not valid (e.g. failed a threshold filter)
                return;
            }
        }

        //Write the data for this time sample
        writeBytes(_byteBuffer); 
    }
    
    /**
     * Load the ByteBuffer with data values for the given variable and time sample.
     */
    private boolean loadTimeSample(TSSVariable var, int timeIndex) {
        //read the value(s) from the given variable for the given time index
        double[] values = var.getValues(timeIndex);
        if (values == null) return false; //no valid data
        
        //Sequence needs special handling to include DAP markers
        if (var.isSequence()) {
            SequenceVariable seq = (SequenceVariable) var;
            int n = seq.getLength(); //number of samples in the sequence
            
            //Get list of projected variables
            List<TSSVariable> vars = seq.getVariables();
            int nvar = vars.size();
            double[] d = new double[nvar];  //assumes all vars in this sequence are scalar doubles, reused

            //Loop over sequence samples
            for (int i=0; i<n; i++) {
                for (int ivar=0; ivar<nvar; ivar++) {
                    int index = i*nvar + ivar;
                    d[ivar] = values[index];
                }
                
                putBytes(START_OF_INSTANCE);
                putDoubles(d);
            }
            
            //end sequence
            putBytes(END_OF_SEQUENCE);
        } else {
            putDoubles(values);
        }
        
        return true;
    }

    /**
     * Put an array of bytes into the ByteBuffer.
     */
    private void putBytes(byte[] bytes) {
        _byteBuffer.put(bytes);
    }
    
    /**
     * Put an array of doubles into the ByteBuffer.
     */
    private void putDoubles(double[] doubles) {
        for (double d : doubles) {
            _byteBuffer.putDouble(d);
        }
    }

}
