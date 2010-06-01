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

import lasp.tss.TimeSeriesDataset;
import lasp.tss.variable.CompositeVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

/**
 * Single header row with variable name and units.
 * 
 * @author Doug Lindholm
 */
public class SimpleHeader implements Header {
    
    /**
     * Add a single header row with variable name and units.
     */
    public String getHeader(AbstractWriter writer, TimeSeriesDataset dataset) {
        TimeSeries ts = dataset.getTimeSeries();
        List<TSSVariable> vars = ts.getVariables();
        String dlm = writer.getProperty("delimiter", ",");
        
        StringBuilder sb = new StringBuilder();
        
        for (TSSVariable var : vars) {
            //if this is a composite variable, do each component
            if (var instanceof CompositeVariable) {
                for (TSSVariable v : ((CompositeVariable) var).getVariables()) {
                    String hdr = makeVariableHeader(v);
                    sb.append(hdr);
                    sb.append(dlm);
                }
            } else {
                String hdr = makeVariableHeader(var);
                sb.append(hdr);
                sb.append(dlm);
            }
        }
        
        sb.setLength(sb.length() - dlm.length()); //remove trailing delimiter
        sb.append(System.getProperty("line.separator"));
        return sb.toString();
    }
    
    private String makeVariableHeader(TSSVariable var) {
        String name = var.getName();
        String units = var.getUnits();
        
        String hdr = name;
        if (units != null) hdr += " (" + units + ")";
        
        return hdr;
    }
}
