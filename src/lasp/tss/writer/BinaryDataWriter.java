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

import java.util.List;

import lasp.tss.TSSException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.TSSVariable;

/**
 * Extend BinaryDataWriter for writing binary data one time sample at a time.
 * 
 * @author Doug Lindholm
 */
public abstract class BinaryDataWriter extends BinaryWriter implements DataWriter {
    
    public void write() {
        TimeSeriesDataset ds = getDataset();
        
        try {
            
            writeHeader();

            //Loop over time samples. write, flush, and check for errors
            int ntim = ds.getLength();
            for (int itim = 0; itim < ntim; itim++) {
                //delegate to the subclass to write each time sample
                writeTimeSample(itim);
            }
            
            writeFooter();
            
        } catch (Throwable t) {
            String msg = getClass().getSimpleName() + " was not able to write the dataset: " + ds.getName();
            throw new TSSException(msg, t);
        }
    }

    /**
     * No-op. Override to write (or do) something before writing any time samples.
     */
    public void writeHeader() {}
    
    /**
     * No-op. Override to write (or do) something after writing all the time samples.
     */
    public void writeFooter() {}

    
  //----- DatasetWriter Methods ----------------------------------------------

    private TimeSeriesDataset _dataset;

    
    public void setDataset(TimeSeriesDataset dataset) {
        _dataset = dataset;
    }
    
    public TimeSeriesDataset getDataset() {
        return _dataset;
    }
    
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
