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

import java.util.LinkedHashSet;
import java.util.Properties;

import lasp.tss.TSSProperties;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.IndependentVariable;
import lasp.tss.variable.SequenceVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;
import lasp.tss.variable.TimeVariable;

/**
 * Writer to respond to the OPeNDAP "html" output option.
 * Present information about the dataset and a query form.
 * 
 * @author Doug Lindholm
 */
public class HtmlFormWriter extends InfoWriter implements DatasetWriter {
    
    public void writeContent() {
        TimeSeriesDataset ds = getDataset();
        
        //write all "info" output also
        super.writeContent();
        
        //write the query form
        println("<h2>Data Set Query Form</h2>");
        println("<blockquote>");
        writeChooser(ds);
        println("</blockquote>");
    }

    /**
     * Write a link to the JavaScript file that processes the query.
     */
    protected void writeScripts() {
        writeScript("tss.js");
    }
    
    /**
     * Write a html selection widget.
     */
    protected void writeChooser(TimeSeriesDataset dataset) {
        StringBuilder sb = new StringBuilder();
        
        //Get time chooser
        TimeSeries ts = dataset.getTimeSeries();
        IndependentVariable tvar = ts.getIndependentVariable();
        sb.append(getChooser(tvar));
        
        //Get chooser for other independent variables
        for (TSSVariable var : ts.getVariables()) {
            if (var.isSequence()) {
                IndependentVariable ivar = ((SequenceVariable) var).getIndependentVariable();
                sb.append(getChooser(ivar));
            }
        }
        
        //Output options (suffix)
        sb.append("Select Output Type: ");
        sb.append("<select id=\"output\">");
        
        //Make unique set of supported suffixes.
        //From  writer.suf.* properties
        Properties props = TSSProperties.getPropertiesStartingWith("writer");
        LinkedHashSet<String> sufs = new LinkedHashSet<String>();
        for (String name : props.stringPropertyNames()) {
            String[] ss = name.split("\\.");
            sufs.add(ss[0]);
        }

        for (String suf : sufs) {
            String desc = TSSProperties.getProperty("writer."+suf+".description");
            //include only those that have a description
            if (desc != null) {
                //Hack to set the default option, could equal anything
                String sel = TSSProperties.getProperty("writer."+suf+".selected");
                if (sel != null) sel = " selected=\"selected\"";
                else sel = "";
                sb.append("<option value=\""+suf+"\"" + sel + ">"+suf+"</option><br/>");
            }
        }
        
        sb.append("</select>");
        sb.append("<br/>\n");
        
        //Submit button, invoke script to make OPeNDAP style request
        sb.append("<input type=\"button\" value=\"Submit\" onclick=\"handle_dataset_request()\"/>");

        //print it
        println(sb.toString());
    }

    
    /**
     * Make the form widget for getting the independent variable range/value.
     * Encapsulate each variable range/value chooser within a form element
     * with the name of the variable. Makes life easier for the script.
     */
    protected String getChooser(IndependentVariable var) {
        //get variable name, use as name of the form to make the js code easier
        String vname = var.getName();
        
        //names for range values
        String v1 = vname+"1";
        String v2 = vname+"2";
        
        //use long name for label, default to name
        String lname = var.getAttributeValue("long_name");
        if (lname == null) lname = vname;

        //get the units for the label
        String units = var.getUnits();
        if (units == null) {
            if (var instanceof TimeVariable) units = "yyyy-mm-dd"; 
            else units = "";
        }
        else {
            if (var instanceof TimeVariable && !units.equals("yyyy-mm-dd")) {
                //don't be redundant
                units = "(yyyy-mm-dd or "+units+")";
            } 
            else units = "("+units+")";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<form name=\""+vname+"\">"); 
        sb.append(lname+" "+units+"<br/>");
        sb.append("Select Range: ");
        sb.append("<input type=\"text\" name=\""+v1+"\" />");
        sb.append("<input type=\"text\" name=\""+v2+"\" />");
        sb.append("<br/>");
        sb.append("</form><br/>\n"); 
        
        return sb.toString();
    }
    
  //----- DatasetWriter Methods ----------------------------------------------

    private TimeSeriesDataset _dataset;


    public void setDataset(TimeSeriesDataset dataset) {
        _dataset = dataset;
    }

    public TimeSeriesDataset getDataset() {
        return _dataset;
    }
      
}
