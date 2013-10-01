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
package lasp.tss.filter;

import lasp.tss.variable.TSSVariable;

/**
 * This filter will replace data values that equal the "missing_value" or "_FillValue"
 * as defined in the ncml. Otherwise, the default missing value is NaN.
 * This will change the "missing_value" or "_FillValue" Attribute.
 * 
 * @author Doug Lindholm
 */
public class ReplaceMissingValueFilter extends ReplacementFilter {
    
    public ReplaceMissingValueFilter(double newValue) {
        setNewValue(newValue);
        double oldValue = getVariable().getMissingValue();
        setOldValue(oldValue);

        resetAttribute(newValue);
    }

    public ReplaceMissingValueFilter() {}

    public void setArguments(String args) throws IllegalArgumentException {
        //args should be replacement value
        double d = Double.parseDouble(args);   
        setNewValue(d);
        double oldValue = getVariable().getMissingValue();
        setOldValue(oldValue);

        resetAttribute(d);
    }

    private void resetAttribute(double value) {
        //Change the attribute if it exists
        TSSVariable var = getVariable();
        String s = var.getAttributeValue("missing_value");
        if (s != null) var.addAttribute("missing_value", ""+value);
        s = var.getAttributeValue("_FillValue");
        if (s != null) var.addAttribute("_FillValue", ""+value);
    }
    
}
