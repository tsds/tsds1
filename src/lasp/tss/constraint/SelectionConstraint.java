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

import java.util.Arrays;

import lasp.tss.TSSPublicException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.filter.Filter;
import lasp.tss.filter.MatchFilter;
import lasp.tss.filter.ThresholdFilter;
import lasp.tss.util.RegEx;
import lasp.tss.variable.CompositeVariable;
import lasp.tss.variable.SequenceVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

import org.apache.log4j.Logger;

import ucar.ma2.Range;

/**
 * Represent a "selection" clause of an OPeNDAP constraint expression.
 * e.g. "time>2009-01-01"
 * 
 * @author Doug Lindholm
 */
public class SelectionConstraint extends Constraint {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(SelectionConstraint.class);
    
    private String _varName;
    private String _operator;
    private String _value;
    
    public SelectionConstraint(String constraintExpression) {
        super(constraintExpression);
        
        String ex = getExpression();
        String[] ss = RegEx.match(ex, RegEx.VARIABLE, RegEx.SELECTION_OPERATOR, ".+");
        _varName = ss[0];
        _operator = ss[1]; 
        _value = ss[2];
    }

    /**
     * If the variable is independent, apply range to all variables.
     * Otherwise, add a filter to the variable named in the selection clause.
     */
    public void constrain(TimeSeriesDataset dataset) {
        TimeSeries timeSeries = dataset.getTimeSeries();
        //Get the variable named in the constraint
        TSSVariable variable = timeSeries.findVariable(_varName);
        if (variable == null) {
            String msg = "Unable to find variable: " + _varName;
            _logger.error(msg);
            throw new TSSPublicException(msg);
        }
        
        //Check if the value is another variable. Null if no such variable.
        TSSVariable var2 = timeSeries.findVariable(_value);

        //If this variable is the independent variable of a Sequence (e.g. time, wavelength)
        //and the value is a constant,
        //apply a subset to each member of the sequence, even if not projected.
        //IndependentVariable-s will be used for length.
        CompositeVariable parent = variable.getParent();
        if (var2 == null && variable.isIndependent() && parent instanceof SequenceVariable) {
            Range range = makeRange(variable);
            parent.subset(range); //subset the seq with the range of its indep var
        }
        
        //otherwise, add a Filter to the named variable to be applied during read/write.
        else {
            Filter filter = null;
            if (variable.isString()) filter = new MatchFilter(_operator, _value);
            else if (var2 == null) filter = new ThresholdFilter(_operator, _value);
            //TODO: else error?
            
            variable.addFilter(filter);
        }
   
    }
    

    /**
     * Define the index range of values that satisfy the expression.
     * Assumes the data are sorted. Intended for Independentvariable-s.
     */
    private Range makeRange(TSSVariable variable) {

        Range range = null;

        try {
            double comparison = variable.parseValue(_value);

            //range indices, start with invalid values;
            int i1 = -1;
            int i2 = -1;

            double[] values = variable.getValues(); //TODO: avoid having to read all values, performance concern
            int n = values.length;
            int index = Arrays.binarySearch(values, comparison);

            if (_operator.contains(">")) {
                i1 = index;
                i2 = n - 1; //max index is the last sample
                if (i1 < 0) i1 = -(i1+1); //if not found, binarySearch returns (-(insertion point) - 1)
                else if (_operator.equals(">")) i1 += 1; //exclude the min if we found a match but are not inclusive ">="
                if (i1 >= n) i1 = -1; //no match 
            } else if (_operator.contains("<")) {
                i1 = 0;
                i2 = index;
                if (i2 < 0) i2 = -(i2+1) - 1; //if not found, binarySearch returns (-(insertion point) - 1), need the sample before the insertion point for "<"
                else if (_operator.equals("<")) i2 -= 1; //exclude the max if we found a match but are not inclusive "<="
            } else if (_operator.equals("=")) {
                if (index >= 0) { //exact match
                    i1 = index;
                    i2 = index;
                }
            } else if (_operator.equals("~")) { //nearest neighbor, not part of OPeNDAP spec
                if (index == -1) i1 = 0; //cval < all, use first
                else if (index < 0) { //no match, find nearest sample
                    i1 = -(index+1) - 1; //before
                    i2 = -(index+1); //after
                    if (i2 >= n) i1 = n - 1; //cval > all, use last
                    else {
                        double d1 = comparison - values[i1]; //diff from lower bound
                        double d2 = values[i2] - comparison; //diff from upper bound
                        if (d2 <= d1) i1 = i2; //note, rounds midpoint up, better for data reported at midday and request is for full day
                    }
                } else i1 = index;

                i2 = i1;
            }


            //Make a Range for this selection, named for the indep var (i.e. coord var, name=dim)
            String name = variable.getName();
            if (i1 != -1 && i2 != -1) range = new Range(name, i1, i2); 
            else range = new Range(name, Range.EMPTY);
        } catch (Exception e) {
            String msg = "Unable to define range for selection: " + toString();
            _logger.error(msg, e);
            throw new TSSPublicException(msg, e);
        } 

        return range;
    }
    
}
