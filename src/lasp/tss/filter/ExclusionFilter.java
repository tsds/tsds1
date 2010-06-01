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
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayObject;
import ucar.ma2.IndexIterator;

/**
 * Abstract Filter that is used to exclude time samples.
 * Subclasses only need to implement the exclusion logic.
 * 
 * @author Doug Lindholm
 */
public abstract class ExclusionFilter extends AbstractFilter {

    protected abstract boolean excludeValue(double value);
    protected abstract boolean excludeString(String string);

    public boolean exclude(Array array) {
        boolean b = false;
        
        //Iterate over each value in the Array. 
        IndexIterator iit = array.getIndexIterator(); 
        while (iit.hasNext()) {
            boolean excluded = false;
            if (array instanceof ArrayDouble) {
                double d = iit.getDoubleNext();
                excluded = excludeValue(d);
            } else if (array instanceof ArrayObject) {
                String s = (String) iit.getObjectNext();
                excluded = excludeString(s);
            }
            
            if (excluded) {
                b = true;
                break;
            }
        }
        
        return b;
    }
            
            
    /**
     * Return the original Array if the exclude method evaluates to false
     * for all values in the Array. Otherwise, return null.
     * The Writer controls the behavior based on these results.
     * Presumably, if any array is null, the time sample won't be written.
     */
    public Array filter(Array array) {
        Array a = array;
        if (exclude(array)) a = null;
        return a;
    }

}
