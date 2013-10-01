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

import lasp.tss.TSSPublicException;
import lasp.tss.variable.IndependentVariable;

import org.apache.log4j.Logger;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

/**
 * Filter to apply an index stride to a variable.
 * Intended for IndependentVariables.
 * 
 * @author Doug Lindholm
 */
public class StrideFilter extends SubsetFilter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(StrideFilter.class);
    
    private int _stride;
    
    public StrideFilter(int stride) {
        _stride = stride;
    }
    
    public StrideFilter() {}
    
    public void setArguments(String args) {
        _stride = Integer.parseInt(args);
    }

    
    protected Range makeRange(IndependentVariable variable) {
        //IndependentVariable variable = (IndependentVariable) getVariable();
        Range range = variable.getRange();
        String name = variable.getName();
        
        int first = range.first();
        int last = range.last();

        try {
            range = new Range(name, first, last, _stride);
        } catch (InvalidRangeException e) {
            String msg = "Unable to define range for stride: " + _stride;
            _logger.error(msg, e);
            throw new TSSPublicException(msg, e);
        }
        
        return range;
    }
    
}











