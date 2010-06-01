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
package lasp.tss.writer;

import java.util.ArrayList;
import java.util.Stack;

import lasp.tss.TSSPublicException;

/**
 * Write the obligatory OPeNDAP error message.
 * It will report the messages of any TSSPublicExceptions that got us here.
 * There's also a link to the help page.
 * 
 * @author Doug Lindholm
 */
public class ErrorWriter extends HtmlWriter {
    
    /*
     * Keep a list of Throwables. 
     */
    private ArrayList<Throwable> _throwables;
    
    /**
     * Construct an ErrorWriter
     */
    public ErrorWriter() {
        _throwables = new ArrayList<Throwable>();
    }
    
    public String getContentDescription() { return "dods-error"; }
    
    /**
     * Write the HTML content.
     */
    public void writeContent() {
        
        println("<b>Time Series Server Error</b>");
        println("<div class=\"error\"><pre><blockquote>");
        
        //Write something useful to the client: messages from TSSPublicException-s.
        //Need to look at all of the "causes" too.
        for (Throwable t : _throwables) {
            Stack<String> msgs = getPublicMessages(t);
            for (String s : msgs) {
                println(s + "</br>");
            }
        }

        println("</blockquote></pre></div>");

        //link to help page
        String url = getServerUrl() + "/help";
        println("For more options, see the <a href=\""+url+"\">help</a> page.");
        
    }

    /**
     * Place messages from "Public" exceptions in a Stack
     * so they can be retrieved in the order that they occurred.
     * Look at the given throwable and its string of causes.
     */
    private Stack<String> getPublicMessages(Throwable t) {
        Stack<String> s = new Stack<String>();
        while (t != null) {
            //keep only if public
            if (t instanceof TSSPublicException) {
                String msg = t.getMessage(); 
                s.push(msg);
            }
            t = t.getCause();
        }
        return s;
    }
    
    /**
     * Add a Throwable to the list.
     */
    public void addThrowable(Throwable t) {
        _throwables.add(t);
    }

}
