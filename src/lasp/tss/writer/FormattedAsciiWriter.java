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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.variable.SequenceVariable;
import lasp.tss.variable.StringVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeVariable;

/**
 * Writer for formatted tabular data output.
 * 
 * @author Doug Lindholm
 */
public class FormattedAsciiWriter extends TextDataWriter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(FormattedAsciiWriter.class);
    
    /**
     * Container for variable format strings.
     */
    private HashMap<String,String> _varFormats;
    
    /**
     * Should the data be presented as a single row for each time sample.
     * This means that Sequences will be collapsed to a column for each sample
     * excluding the independent variable.
     */
    private boolean _flatten = false;
    
    /**
     * Delimiter
     */
    private String _delim; 

    /**
     * Initialize the Writer.
     */
    public void init() {
        //set up a few things so we don't have to do them for each time sample.
        _delim = getProperty("delimiter", ","); //default to comma
        _varFormats = new HashMap<String,String>();
        String flat = getProperty("flatten", "false");
        _flatten = Boolean.parseBoolean(flat);
    }
    
    /**
     * Write the data for the given time sample.
     */
    public void writeTimeSample(int timeIndex) {
        
        StringBuilder sb = new StringBuilder();
        SequenceVariable seq = null;
        
        try {           
            String format = null;
            String seqformat = null;
            List<TSSVariable> vars = getVariables();
            for (TSSVariable var : vars) {
                format = getFormatString(var); //string defining the format to use for output
                
                //Unflattened sequence will go into the last columns.
                //It will repeat other vars for each sequence sample.
                if (var.isSequence() && !_flatten) {
                    seq = (SequenceVariable) var; //save it and do it last, 
                    seqformat = format;
                    continue;
                }

                String vstring = null; //formatted string representation of this variable's data
                if (var instanceof StringVariable) {
                    String[] strings = var.getStringValues(timeIndex);
                    //If null (e.g. filtered out) skip the whole time sample
                    if (strings == null) return;
                    vstring = format(format, strings);
                } else if (var instanceof TimeVariable) {
                    //TODO: if already formatted, get String value, but let Writer override format?
                    double[] dd = var.getValues(timeIndex);
                    if (dd == null) return; //Skip writing this time sample if there are no time values
                    double d = dd[0];
                    if (Double.isNaN(d)) return; //Skip writing this time sample if the time value is undefined, a little trick to exclude samples
                    vstring = formatTime(format, d);
                } else { 
                    if (var.isSequence() && _flatten) {
                        //un-project the indep var
                        ((SequenceVariable) var).getIndependentVariable().setProjected(false);
                    }
                    double[] values = var.getValues(timeIndex);

                    //If null (e.g. filtered out) skip the whole time sample
                    if (values == null) return;
                    vstring = format(format, values);
                }
                
                sb.append(vstring);
            }

            String s = null;
            if (seq != null) { //not flattened, one row for each sequence sample, repeating what we have so far
                s = formatSequenceTimeSample(seqformat, seq, timeIndex, sb.toString());
                if (s == null) return;
            } else {
                removeTrailingDelimiter(sb);
                s = sb.toString();
            }
            println(s); 

        } catch (Throwable t) {
            String msg = "Unable to write the data for timeIndex: " + timeIndex;
            _logger.error(msg, t);
            throw new TSSException(msg, t);
        }

    }
    
    /**
     * Put the output for a time sample into String. One row for every sequence sample. All previous data will be repeated.
     */
    private String formatSequenceTimeSample(String format, SequenceVariable seq, int timeIndex, String prefix) {
        StringBuilder sb = new StringBuilder();
        
        double[] values = seq.getValues(timeIndex); 
        if (values == null) return null;
         
        int nsamp = seq.getLength(); //one row for each sample
        int nvar = seq.getVariableCount(); //number of variables

        //loop over seq samples, fill the array for that row, start each row with prefix
        for (int isamp=0; isamp<nsamp; isamp++) { 
            sb.append(prefix); //start with all the data to repeat for each sample
            
            double[] d = new double[nvar];
            for (int ivar=0; ivar<nvar; ivar++) { 
                int index = isamp*nvar+ivar;
                d[ivar] = values[index];
            }
            String f = format(format, d);
            sb.append(f);
            
            removeTrailingDelimiter(sb);

            if (isamp < nsamp-1) sb.append(System.getProperty("line.separator")); //new line for each sequence sample
        }

        return sb.toString();
    }
    
    /**
     * Remove the trailing delimiter from the given line of text.
     */
    private void removeTrailingDelimiter(StringBuilder line) {        
        int i = line.lastIndexOf(_delim);
        if (i > 0) line.setLength(i);
    }
    
    /**
     * Return the time value in the given format.
     */
    protected String formatTime(String format, double time) {
        TimeVariable tvar = getDataset().getTimeVariable();
        
        String formattedTime = null;
        if (format != null) {
            if (format.startsWith("%")) formattedTime = format(format, time); //like any other number
            else formattedTime = tvar.format(format, time); //special date format
        } else {
            //no formatting
            formattedTime = Double.toString(time);
        }
        
        return formattedTime;
    }
 
    /**
     * Return the format string to use for the given variable.
     */
    protected String getFormatString(TSSVariable variable) {
        String vname = variable.getName();
        
        //see if we already defined the format for this variable
        String format = _varFormats.get(vname); 
        
        if (format == null) {
            //Look for an explicit format definition. 
            if (variable instanceof TimeVariable) format = getProperty("time.format");
            if (format == null) format = getProperty("format"); //from Writer's config (tsds.properties)
            if (format == null && variable instanceof TimeVariable) format = ((TimeVariable) variable).getFormat();
            if (format == null) format = variable.getAttributeValue("format"); //from ncml variable attributes
            if (format == null) format = variable.getAttributeValue("cformatstring"); //Bob's request
            
            //If the format is not explicitly defined...
            if (format == null) {
                if (variable.isString()) format = "%s"; //generic String
                else if (variable.isSequence()) { 
                    StringBuilder sb = new StringBuilder();
                    SequenceVariable seq = (SequenceVariable) variable;
                    for (TSSVariable var : seq.getVariables()) {
                        //don't include indep var in flattened output
                        if (_flatten && var.isIndependent()) continue;
                        
                        String f = getFormatString(var); //recursive
                        sb.append(f);
                    }
                    removeTrailingDelimiter(sb);
                    format = sb.toString();
                } 
                else { //Scalar
                    format = "%f"; //general float
                    
                    String precision = variable.getAttributeValue("precision"); //number of decimal places
                    //Make sure this is a valid precision, must be an integer.
                    //If not valid, log warning and use default format.
                    if (precision != null) {
                        try {
                            Integer.parseInt(precision);
                            format = "%."+precision+"f"; //float with fixed number of decimal places
                        } catch (Exception e) {
                            _logger.warn("Invalid precision: " + precision);
                        }
                    }
                     
                    String sigfig = variable.getAttributeValue("sigfig"); //significant figures
                    //Make sure this is a valid value, must be an integer.
                    //If not valid, log warning and use default format.
                    if (sigfig != null) {
                        try {
                            Integer.parseInt(sigfig);
                            format = "%."+sigfig+"g"; //float, scientific notation if appropriate
                        } catch (Exception e) {
                            _logger.warn("Invalid number of significant figures: " + sigfig);
                        }
                    }

                    //TODO: warn if both precision and sigfig are specified
                }
            }
            
            //Create the complete format string for the row.
            StringBuilder sb = new StringBuilder();
            
            //Figure out how many values we'll have
            int n = variable.getNumDependentVariables();

            //If "flattened" Sequence, each sample will get its own column.
            //Format should be complete for all the vars in one sample.
            if (variable.isSequence() && _flatten) n = ((SequenceVariable) variable).getLength(); //number of samples

            //If non-flattened Sequence, build up format string from each component, use Sequence format as default
            if (variable.isSequence() && !_flatten) {
                SequenceVariable seq = (SequenceVariable) variable;
                for (TSSVariable var : seq.getVariables()) {
                    String f = getFormatString(var); //recursive
                    //TODO: this will return the default "%f" if not defined, instead of inheriting the seq format
                    if (f == null) f = format;
                    sb.append(f);
                }
            } else {
                //repeat format for each value
                for (int i=0; i<n; i++) sb.append(format).append(_delim); 
            }

            format = sb.toString(); 

            //cache the format for each variable so we don't have to go through this again.
            _varFormats.put(vname, format);
        }
        
        return format;
    }

    protected String getDelimiter() {
        return _delim;
    }
    
}
