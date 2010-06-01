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

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for Writers which are responsible for writing the Servlet response.
 * 
 * @author Doug Lindholm
 */
public abstract class AbstractWriter implements Writer { 

    /**
     * Each Writer maintains its own set of properties.
     * Typically (only?) from the tss.properties file.
     * The property names originally start with "writer.<suffix>"
     * but that part will be dropped for these properties.
     */
    private Properties _properties;
        
    /**
     * Return the mime type expression used in the http "Content-Type" header.
     */
    public abstract String getContentType();


    /**
     * No-op. Override to initialize the Writer before anything gets written.
     */
    public void init() {}
    
    /**
     * Return the value for the Content-Description http header.
     */
    public String getContentDescription() { return null; }
    
    /**
     * Hook for the WriterFactory to set the properties.
     */
    public void setProperties(Properties props) {
        _properties = props;
    }
    
    /**
     * Return the value of the given property. Will return null if not defined.
     */
    public String getProperty(String name) {
        String value = null;
        if (_properties != null) value = _properties.getProperty(name);
        return value;
    } 
    
    /**
     * Return the value of the given property. Will return the defaultValue if not defined.
     */
    public String getProperty(String name, String defaultValue) {
        String value = null;
        if (_properties != null) value = _properties.getProperty(name, defaultValue);
        return value;
    } 
    
    
    //Hack so writers have access to more info.
    protected HttpServletRequest _request;
    protected HttpServletResponse _response;
    
    public void setRequest(HttpServletRequest request) {_request = request;}
    
    public void setResponse(HttpServletResponse response) {_response = response;}

    /**
     * Get the base URL (up through TSS servlet mapping) from the request
     */
    protected String getServerUrl() {
        String url = _request.getRequestURL().toString(); //full url (without query) 
        String pinfo = _request.getPathInfo(); // data set name + suffix
        
        //Remove the path info from the url.
        //The test accounts for hitting just the servlet with no ending "/"
        if (pinfo != null && url.endsWith(pinfo)) {
            int index = url.lastIndexOf(pinfo);
            url = url.substring(0,index);
        }
        
        return url;
    }
    
}
