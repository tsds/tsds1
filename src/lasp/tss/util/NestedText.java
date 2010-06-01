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

/**
 * Helper Class for making the OPeNDAP DAS and DDS.
 * 
 * @author Doug Lindholm
 */
public class NestedText {

    protected StringBuilder _text;
    private int _level = 0;
    private static int TAB_SIZE = 4;
    
    public NestedText() {
        _text = new StringBuilder();
    }
    
    /**
     * Add a line with the appropriate level of indentation.
     * This manages the indentation level based on "{" or "}" block indicators. 
     * Note that a block must be ended on a new line. 
     * e.g. the line closing the block must start with a "}".
     * This method will also append a line separator.
     */
    public void append(String line) {
        line = line.trim(); //we'll manage end spaces here
        
        if (line.startsWith("}")) _level--;  //end block, un-indent before we append this
        
        int n = _level * TAB_SIZE; //number of spaces needed to indent
        for (int i=0; i<n; i++) _text.append(" "); //append n spaces to start this line
        _text.append(line);
        _text.append(System.getProperty("line.separator")); //new line
        
        if (line.endsWith("{")) _level++;  //start new block, indent next time
    }
    
    public String toString() {
        return _text.toString();
    }
    
}
