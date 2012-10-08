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
package lasp.tss.constraint;

import org.apache.log4j.Logger;

import lasp.tss.TSSProperties;
import lasp.tss.TSSPublicException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.filter.Filter;
import lasp.tss.filter.TimeSeriesFilter;
import lasp.tss.util.RegEx;
import lasp.tss.variable.CompositeVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

/**
 * A Constraint that represents a "function" in an OPeNDAP constraint expression.
 * Has the form: filter(args) where args is optional. args is simply a String.
 * Intended for functions defined in the selection clause
 * that can be applied to each time sample as it is read.
 * The result is the same variable but with the filter applied
 * (as opposed to creating a new variable, not yet supported).
 * 
 * @author Doug Lindholm
 */
public class FilterConstraint extends Constraint {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(FilterConstraint.class);
    
    public FilterConstraint(String expression) {
        super(expression);
    }

    /**
     * Apply the Filter to the dataset.
     */
    public void constrain(TimeSeriesDataset dataset) {

        //Make the Filter
        TimeSeries ts = dataset.getTimeSeries();
        Filter filter = makeFilter(ts);
        
        //If for a time subset, apply to the TimeSeries Sequence
        if (filter instanceof TimeSeriesFilter) {
            ((TimeSeriesFilter) filter).filter(ts);
        } else {
            //hack will ignore the filter we just made
            addFilter(ts); 
        }
    }
    
    /**
     * Add the Filter to all Variables, recursively.
     * HACK: some filters need access to the Variable
     * so make a new instance of the Filter for each var.
     */
    private void addFilter(TSSVariable var) {
        Filter filter = makeFilter(var);     //TODO: making filter for ts twice!  added to var only once, mostly harmless
        //filter.setVariable(var);
        var.addFilter(filter);
        if (var instanceof CompositeVariable) {
            for (TSSVariable v : ((CompositeVariable) var).getVariables()) {
                addFilter(v); //recursive
            }
        }
    }
    
    /**
     * Construct a Filter based on the call in the request's constraint expression.
     * HACK: some filters need access to the Variable
     */
    private Filter makeFilter(TSSVariable var) {
        Filter filter = null;
        
        String ex = getExpression();
        String[] ss = RegEx.match(ex, RegEx.WORD, "\\(", ".*", "\\)");
        String filterName = ss[0];
        String args = ss[2];
        
        String className = TSSProperties.getProperty("filter."+filterName+".class"); 
        if (className == null) {
            String msg = "No filter definition found for: " + ex;
            throw new TSSPublicException(msg);
        }
        
        try {
            Class fclass = Class.forName(className);
            filter = (Filter) fclass.newInstance();
            filter.setVariable(var); 
            filter.setArguments(args);
            
        } catch (Exception e) {
            String msg = "Unable to construct Filter: " + className;
            _logger.error(msg, e);
        } 
        
        return filter;
    }

}
