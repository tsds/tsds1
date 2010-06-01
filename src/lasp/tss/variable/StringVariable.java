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
package lasp.tss.variable;

import ucar.ma2.Array;
import ucar.nc2.Variable;

/**
 * A Variable to represent text data. 
 * 
 * @author Doug Lindholm
 */
public class StringVariable extends TSSVariable {
    
    /**
     * Construct a StringVariable based on a nc2.Variable,
     * presumably of type "String".
     */
    public StringVariable(CompositeVariable parent, Variable ncVariable) {
        super(parent, ncVariable);
    }
    
    /**
     * Override to return a NaN as the double representation of the string.
     */
    public double parseValue(String s) {
        return Double.NaN;
    }

    /**
     * Override to return null for the double representation of the data values.
     */
    public double[] getValues() {return null;}
    
    /**
     * Override to return null for the double representation of the data values.
     */
    public double[] getValues(int timeIndex) {return null;}

    /**
     * Return all data values as an array of String-s.
     */
    public String[] getStringValues() {
        String[] s = null;

        Array array = read();
        Object[] os = (Object[]) array.get1DJavaArray(String.class);

        int n = os.length;
        int i = 0;
        s = new String[n];
        for (Object o : os) s[i++] = (String) o;

        return s;
    }
    
    /**
     * Return the data values for the given time sample as an array of String-s.
     */
    public String[] getStringValues(int timeIndex) {
        String[] s = null;

        Array array = getTimeSample(timeIndex);
        if (array != null) {
            Object[] os = (Object[]) array.get1DJavaArray(String.class);
            int n = os.length;
            int i = 0;
            s = new String[n];
            for (Object o : os) s[i++] = (String) o;
        }

        return s;
    }
    
    /**
     * Return the data type for the DDS.
     */
    public String getDodsType() {
        return "String";
    }

}
