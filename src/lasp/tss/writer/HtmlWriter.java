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

/**
 * Abstract Writer with implementation for HTML output.
 * 
 * @author Doug Lindholm
 */
public abstract class HtmlWriter extends TextWriter {

    /**
     * Return the title for the page.
     */
    protected String getTitle() {return "Time Series Server";} //TODO: config
    
    /**
     * Write the content of the body. None by default.
     */
    protected void writeContent() {}
    
    /**
     * Write script to the head. None by default.
     */
    protected void writeScripts() {}

    /**
     * Return the html mime type.
     */
    public String getContentType() { return "text/html"; }
    
    /**
     * Write the HTML doctype, head, and body.
     */
    public void write() {
        println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
        
        writeHead();
        writeBody();
        
        println("</html>");
    }
    
    /**
     * Write the head portion of the HTML.
     */
    protected void writeHead() {
        println("<head>");
        
        String title = getTitle();
        println("<title>" + title + "</title>");
        
        //add link to style sheet, get via file servlet
        String cssurl = getServerUrl() + "/resources/tss.css";
        println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssurl + "\">");
        
        //add Script
        writeScripts();
        
        println("</head>");
    }
    
    /**
     * Write the body portion of the HTML.
     */
    protected void writeBody() {
        println("<body>");
        writeContent();
        println("</body>");
    }
    
    /**
     * Write a "script" element with a src URL for the JavaScript with the given name.
     * The script must live under the baseURL of the FileServlet,
     * which is mapped to with "resources" preceded by the URL to the TSS Servlet.
     */
    protected void writeScript(String file) {
        String jsurl = getServerUrl() + "/resources/" + file;
        String s = "<script type=\"text/javascript\" src=\""+jsurl+"\"></script>";
        println(s);
    }
    
    /**
     * Write the given content within a div element with the given style class.
     */
    protected void printDiv(String cssClass, String content) {
        println("<div class=\"" + cssClass + "\">");
        println(content);
        println("</div>");
    }
    
}
