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
 * This filter will exclude data values (entire time sample) that match the given value.
 * 
 * @author Doug Lindholm
 */
public class ExcludeValueFilter extends ExclusionFilter {
    
    private double _value; 
    private String _svalue; 
    
    public ExcludeValueFilter(double value) {
        _value = value;
        _svalue = ""+value; //for the sake of completeness
    }  
    
    public ExcludeValueFilter(String value) {
        _svalue = value;
        
        //get as double, if we can
        //TODO: default value?
        try {
            _value = Double.parseDouble(value);
        } catch (NumberFormatException e) {}
    }   
    
    protected boolean excludeValue(double value) {
        boolean b = false;
        
        boolean bothNaN = Double.isNaN(_value) && Double.isNaN(value); //NaN != NaN
        if (bothNaN || value == _value) b = true;
        
        return b;
    }
    
    protected boolean excludeString(String string) {
        boolean b = false;
        
        //match when both are null, avoid null pointer when one is null
        boolean bothNull = (_svalue == null) && (string == null);
        if (bothNull || (_svalue != null && _svalue.equals(string))) b = true;
        
        return b;
    }

}
