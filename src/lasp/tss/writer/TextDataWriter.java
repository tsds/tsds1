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

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.TSSVariable;

/**
 * Abstract class to support writing data as text, one time sample at a time.
 * 
 * @author Doug Lindholm
 */
public abstract class TextDataWriter extends TextWriter implements DataWriter {
 
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TextDataWriter.class);
            
    public void write() {
        TimeSeriesDataset ds = getDataset();
        
        try {
            //If the time.format property is defined, modify the Time Variable's 
            //units metadata so we get the header right.
            //Note, this takes precedence over a user call to the format_time filter.
            String format = getProperty("time.format");
            if (format != null) ds.getTimeVariable().setFormat(format);
            
            writeHeader();
                
            //Loop over time samples. write
            int ntim = ds.getLength();
            for (int itim = 0; itim < ntim; itim++) {
                //delegate to the subclass to write each time sample
                writeTimeSample(itim);
            }
            
            writeFooter();
            
        } catch (Throwable t) {
            String msg = "Unable to write the data set: " + _dataset.getName();
            _logger.error(msg, t);
            throw new TSSException(msg, t);
        }
    }


    /**
     * Write (or do) something before writing any time samples.
     * Look for a Header class to create the header content.
     */
    public void writeHeader() {
        String hdrClass = getProperty("header");
        if (hdrClass == null) return;
        String[] hdrs = hdrClass.split(",");

        for (String hclass : hdrs) {
            //Construct the Header class
            Header header = null;
            try {
                Class c = Class.forName(hclass);
                header = (Header) c.newInstance();
            } catch (Throwable t) {
                String msg = "Failed to construct Header class: " + hclass;
                throw new TSSException(msg, t);
            }

            String hdr = header.getHeader(this, getDataset());
            if (hdr != null) print(hdr);
        }
    }
    
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
