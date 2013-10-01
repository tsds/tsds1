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

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TimeSeriesDataset;

/**
 * Writer for the OPeNDAP Dataset Attribute Structure.
 * 
 * @author Doug Lindholm
 */
public class DasWriter extends TextWriter implements DatasetWriter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(DasWriter.class);
    
    /**
     * Return String to be used in the "Content-Description" header.
     */
    public String getContentDescription() {
        return "dods-das";
    }

    /**
     * Write the DAS representation of the dataset.
     */
    public void write() {
        //Get the DAS as a String from the Dataset
        TimeSeriesDataset ds = getDataset();
        
        try {
            String das = _dataset.getDAS();
            print(das);
        } catch (Exception e) {
            String msg = "Unable to write the DAS for: " + ds.getName();
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
    }

  //----- DatasetWriter Methods ----------------------------------------------

    private TimeSeriesDataset _dataset;

    /**
     * Hook to get the Dataset just after construction of the Writer.
     */
    public void setDataset(TimeSeriesDataset dataset) {
        _dataset = dataset;
    }

    public TimeSeriesDataset getDataset() {
        return _dataset;
    }
      
}
