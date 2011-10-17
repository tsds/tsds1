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
 * This filter will exclude data values (entire time sample) that match the Variable's "_FillValue" attribute.
 * 
 * @author Doug Lindholm
 */
public class ExcludeMissingValueFilter extends ExclusionFilter {
    
    private double _missingValue; //cache it so we don't have to get it for each time sample
    private boolean cached = false;
    
    protected boolean excludeValue(double value) {
        boolean b = false;
        
        double mv = getMissingValue();
        boolean bothNaN = Double.isNaN(mv) && Double.isNaN(value); //NaN != NaN
        if (bothNaN || value == mv) b = true;
        
        return b;
    }
    
    /**
     * No-op to satisfy interface.
     */
    protected boolean excludeString(String string) {
        return false;
    }

    /**
     * Get missing value from Variable attributes.
     * Default is NaN.
     */
    private double getMissingValue() {
        
        if (! cached) {
            _missingValue = getVariable().getMissingValue();
            cached = true;
        }
        
        return _missingValue;
    }

}
