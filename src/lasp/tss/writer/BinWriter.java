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

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.variable.TSSVariable;

/**
 * Write data as flat binary with each value represented as a Little Endian IEEE double
 * Time dimension varyies slowest.
 * 
 * @author Doug Lindholm
 */
public class BinWriter extends BinaryDataWriter {
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(BinWriter.class);
    
    /**
     * Store data temporarily in a ByteBuffer.
     */
    private ByteBuffer _byteBuffer;

    /**
     * Initialize the writer. Compute the number of bytes needed for a time sample.
     * Initialize a reusable ByteBuffer.
     */
    public void init() {
        int size = 0;
        
        for (TSSVariable var : getVariables()) {
            size += var.getTimeSampleSizeInBytes();
        }
        
        _byteBuffer = ByteBuffer.allocate(size);
        _byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }


    /**
     * Write all projected variables for the given time sample.
     */
    public void writeTimeSample(int timeIndex) {
        try {
            _byteBuffer.rewind(); //reuse
            for (TSSVariable var : getVariables()) {
                double[] values = var.getValues(timeIndex);
                //If null (e.g. filtered out) skip the whole time sample
                if (values == null) return;
                for (double d : values) {
                    _byteBuffer.putDouble(d);
                }
            }
            
            writeBytes(_byteBuffer);
        } catch (Exception e) {
            String msg = "Unable to write the time sample for timeIndex: " + timeIndex;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
    }

}
