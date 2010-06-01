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

import lasp.tss.TSSException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.TSSVariable;

/**
 * Read/write binary data in a single chunk.
 * Same format as BinWriter.
 * Supports dataset's with only one variable.
 * Supports only time subsetting constraints.
 * This is an experiment for improving performance.
 * 
 * @author Doug Lindholm
 */
public class FastBinWriter extends BinaryWriter implements DatasetWriter {

    public void write() {
        TimeSeriesDataset ds = getDataset();
        
        try {
            TSSVariable var = getVariables().get(0); //first var, assume only one
            double[] values = var.getValues();
            
            int size = values.length * 8;
            ByteBuffer bb = ByteBuffer.allocate(size);
            bb.order(ByteOrder.LITTLE_ENDIAN); 
            
            for (double d : values) bb.putDouble(d);
            writeBytes(bb);
            
        } catch (Throwable t) {
            String msg = getClass().getSimpleName() + " was not able to write the data set: " + ds.getName();
            throw new TSSException(msg, t);
        }
        
    }


  //----- DatasetWriter Methods ----------------------------------------------
    
    private TimeSeriesDataset _dataset;
    public void setDataset(TimeSeriesDataset dataset) {_dataset = dataset;}
    public TimeSeriesDataset getDataset() {return _dataset;}

    /**
     * List of variables to write. 
     * Cache here to minimize work in time loop.
     */
    private List<TSSVariable> _variables;
    
    public List<TSSVariable> getVariables() {
        if (_variables == null) _variables = _dataset.getTimeSeries().getVariables();
        return _variables;
    }
}
