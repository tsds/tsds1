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
import ucar.ma2.Array;

/**
 * Interface for "filters" that take one or more arrays as input
 * and return a single array.
 * 
 * @author Doug Lindholm
 */
public interface Filter {
        
    /**
     * Make sense of the filter arguments when presented as a string.
     * Could be a comma separated list.
     * Used with no-arg constructor for filters defined in the request.
     * Throw an IllegalArgumentException if args aren't valid.
     */
    public void setArguments(String args) throws IllegalArgumentException;
    
    /**
     * Apply the filter to the Array and return the result.
     * Intended for single time sample. Bordering on contract. Other usage is not tested.
     * Always returns a new array unless the filter is a no-op.
     */
    public Array filter(Array array);

    /**
     * Give the Filter access to the variable it is tied to.
     */
    public void setVariable(TSSVariable variable);
    public TSSVariable getVariable();

}
