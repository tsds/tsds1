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
package lasp.tss.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regular expression utilities. Defines some common patterns and
 * provides a method that simplifies getting matching strings.
 * 
 * @author Doug Lindholm
 */
public class RegEx {

    /**
     * Regular expression matching one or more word characters ([a-zA-Z_0-9]).
     */
    public static final String WORD = "\\w+";
    
    /**
     * Regular expression matching a variable name.
     * Limited to alpha-numeric characters and underscore.
     * Nested components may have "." in the name.
     */
    public static final String VARIABLE = WORD+"(\\."+WORD+")*";

    /**
     * Regular expression matching an OPeNDAP "function" constraint expression.
     * Limited to a word (alpha-numeric characters and underscore)
     * followed by parentheses optionally containing arguments.
     */
    public static final String FUNCTION = RegEx.WORD+"\\(.*\\)";
    
    /**
     * Regular expression matching the operators: 
     *   >   Greater than
     *   >=  Greater than or equal to
     *   <   Less than
     *   <=  Less than or equal to
     *   =   Equals
     *   !=  Not equals
     *   =~  Matches pattern
     *   ~   Almost equals, match nearest value
     */
    public static final String SELECTION_OPERATOR = "(>=|>|<=|<|=~|=|!=|~)"; 

    /**
     * Regular expression matching an OPeNDAP constraint expression selection clause.
     */
    public static final String SELECTION = VARIABLE + SELECTION_OPERATOR + ".+";
    
    /**
     * Hyperslab definition. Square brackets with something in them.
     * [i], [i:j], [i:j:k]
     */
    public static final String HYPERSLAB = "\\[\\d+(:\\d+(:\\d+)?)?\\]";
    
    /**
     * Regular expression matching an OPeNDAP constraint expression projection clause.
     * Comma separated list of variables with optional hyperslab.
     */
    public static final String PROJECTION = VARIABLE+"("+RegEx.HYPERSLAB+")?";

    /**
     * Regular expression that should match any reasonable number
     * including scientific notation.
     */
    public static final String NUMBER = "[+|-]?[0-9]*\\.?[0-9]*([e|E][+|-]?[0-9]*)?";
    
    /**
     * Regular expression that should match an ISO 8601 time.
     * No fractional seconds. Not variants without "-".
     */
    public static final String TIME = "[0-9]{4}-[0-9]{2}-[0-9]{2}(T[0-2][0-9](:[0-5][0-9](:[0-5][0-9])?)?)?";

    /**
     * Match common delimiters: white space and commas.
     */
    public static final String DELIMITER = "[\\s,]+"; //white space and commas
    
    
    
    /**
     * Build up a regular expression from the "strings" and match to "string".
     * If match, return matching string for each of the regex components (strings).
     * If no match, return null.
     */
    public static String[] match(String string, String... strings) {
        String[] matches = null;
        
        int n = strings.length;
        
        //Build up the complete regular expression string.
        StringBuilder regex = new StringBuilder();
        int i = 0; //index of matches
        int[] groupIndex = new int[n]; //matching group index, 1-based
        int igrp = 1; //counter for tracking group index
        for (String s : strings) {
            groupIndex[i] = igrp;
            
            //Count the number of groups within this regex component
            //so we can keep track of the matching group index.
            //Do by counting group openings: "("
            char[] chars = s.toCharArray();
            boolean skip = false;
            for (char c : chars) {
                if (skip) {
                    skip = false;
                    continue;
                }
                
                if ('\\' == c) skip=true;  // don't count escaped "("
                if ('(' == c) igrp++;
            }
            
            //Wrap each regex string as a group that we want to match.
            regex.append("(");
            regex.append(s);
            regex.append(")");
            
            igrp++; //We just added another group
            i++; //ready for next regex component
        }
        
        //Try to match.
        String re = regex.toString();
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(string);
        if (m.find()) {
            //If we matched, populate the result array.
            matches = new String[n];
            for (int j=0; j<n; j++) matches[j] = m.group(groupIndex[j]);
        }

        return matches;
    }
    
}
