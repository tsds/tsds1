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

import java.io.File;

import lasp.tss.TSSProperties;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.TimeSeries;

/**
 * Output information about the given Dataset.
 * 
 * @author Doug Lindholm
 */
public class InfoWriter extends HtmlWriter implements DatasetWriter {

    /**
     * Return the title of the Dataset.
     */
    protected String getTitle() {
        TimeSeriesDataset ds = getDataset();
        TimeSeries ts = ds.getTimeSeries();
        String title = ts.getAttributeValue("title");
        
        return title;
    }

    /**
     * Write the content of the body. 
     */
    protected void writeContent() {
        TimeSeriesDataset ds = getDataset();

        //header with dataset title
        println("<h1>"+getTitle()+"</h1>");
        
        //content of an "info" file if present, same name as dataset with ".info" extension
        String dir = TSSProperties.getDatasetDir();
        String dsname = ds.getName();
        String fname = dir + dsname + ".info";
        File file = new File(fname);
        if (file.exists()) {
            String f = readFile(fname);
            println("<blockquote>");
            printDiv("info", f);
            println("</blockquote>");
        }
        
        //OPeNDAP Dataset Descriptor Structure
        String dds = ds.getDDS();
        dds = "<h2>Dataset Descriptor Structure</h2><blockquote>" + dds + "</blockquote>";
        printDiv("dds", dds);

        //OPeNDAP Dataset Attribute Structure
        String das = ds.getDAS();
        das = "<h2>Dataset Attribute Structure</h2><blockquote>" + das + "</blockquote>";
        printDiv("das", das);
        
    }

    
  //----- DatasetWriter Methods ----------------------------------------------
    private TimeSeriesDataset _dataset;
    public void setDataset(TimeSeriesDataset dataset) { _dataset = dataset; }
    public TimeSeriesDataset getDataset() { return _dataset; }

}
