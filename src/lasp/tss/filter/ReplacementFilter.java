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

import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

/**
 * This filter will replace data values as defined in the replaceValue method.
 * 
 * @author Doug Lindholm
 */
public class ReplacementFilter extends AbstractFilter {

    protected double _oldValue, _newValue;
    
    public ReplacementFilter(double oldValue, double newValue) {
        _oldValue = oldValue;
        _newValue = newValue;
    }
    
    public ReplacementFilter() {}

    public void setArguments(String args) throws IllegalArgumentException {
        //args should be comma separated values
        String[] ss = args.split(",");
        _oldValue = Double.parseDouble(ss[0]);
        _newValue = Double.parseDouble(ss[1]);        
    }
    
    /**
     * If this value matches the "old value", return the value to replace it.
     */
    protected double replaceValue(double value) {
        double d = value;
        //Deal with case where both values are NaN (NaN != NaN)
        boolean bothNaN = Double.isNaN(_oldValue) && Double.isNaN(value);
        if (bothNaN || value == _oldValue) d = _newValue;
        return d;
    }
    
    /**
     * Run every value in the Array through the "replace" filter method.
     * Return a new Array with the new values.
     */
    public Array filter(Array array) {
        Array arr = array;
        
        //Only try to apply it if the Array contains doubles, 
        // e.g. not strings or formatted times
        if (! (array instanceof ArrayObject)) {
            int n = (int) array.getSize();
            double[] data = new double[n];
            
            //Iterate over each value in the Array. 
            IndexIterator iit = array.getIndexIterator(); 
            int i=0;
            while (iit.hasNext()) {
                double d = iit.getDoubleNext();
                data[i++] = replaceValue(d);
            }
            
            int[] shape = array.getShape();
            arr = Array.factory(DataType.DOUBLE, shape, data);
        }
        
        return arr;
    }


    /**
     * No-op. Designed to filter single time samples of dependent variables.
     */
    public void filter() {}
    
    /**
     * Let subclasses set state.
     */
    protected void setNewValue(double newValue) {
        _newValue = newValue;        
    }
    
    /**
     * Let subclasses set state.
     */
    protected void setOldValue(double oldValue) {
        _oldValue = oldValue;        
    }
}
