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

/**
 * Like the SubsetFilter but for non-sorted, non-independent variables.
 * Each value must be tested. The intent is for the entire time sample
 * to be dropped if any part doesn't pass the filter.
 * 
 * @author Doug Lindholm
 */
public class ThresholdFilter extends ExclusionFilter {

    private String _operator;
    private double _threshold;
    
    boolean _lt, _eq, _gt;
    
    public ThresholdFilter(String operator, String threshold) {
        _operator = operator;
        _threshold = Double.parseDouble(threshold);
        
        _lt = _operator.contains("<");
        _eq = _operator.contains("=");
        _gt = _operator.contains(">");
    }    

    public ThresholdFilter() {}
    
    /**
     * Compare the Array value(s) (typically one time sample) to this Filter's threshold.
     * Return true if the input data fails to pass the operator test.
     * Override this instead of exclude
     */
    public boolean excludeValue(double value) {
        int result = Double.compare(value, _threshold);
        boolean excluded = isExcluded(result);
        return excluded;
    }

    public boolean excludeString(String string) {
        int result = string.compareToIgnoreCase(""+_threshold);
        boolean excluded = isExcluded(result);
        return excluded;
    }
    
    private boolean isExcluded(int compareResult) {
        boolean excluded = false; 

        if (compareResult <  0 && !_lt) excluded = true;
        if (compareResult == 0 && !_eq) excluded = true;
        if (compareResult >  0 && !_gt) excluded = true;

        return excluded;
    }

}
