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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TSSProperties;
import lasp.tss.TSSPublicException;

/**
 * Factory to construct implementation of a Writer interface.
 * The tss.properties file maps the "suffix" to the specific Writer class.
 * 
 * @author Doug Lindholm
 */
public class WriterFactory {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(WriterFactory.class);
    
    /**
     * Construct a Writer for the given output type (i.e. suffix).
     * The response is needed to get the ServletOutputStream or PrintWriter.
     * The request is the handiest source of other useful information.
     */
    public static AbstractWriter makeWriter(String type, HttpServletRequest request, HttpServletResponse response) {
        AbstractWriter writer = null;
        
        //Get properties for this writer, strip off leading part of names
        Properties props = TSSProperties.getPropertiesStartingWith("writer."+type);
        
        //Get the Writer class name
        String cname = props.getProperty("class");
        if (cname == null) {
            String msg = "No Writer class definition was found for the requested output type: " + type;
            _logger.error("The writer."+type+".class property is not defined.");
            throw new TSSPublicException(msg);
        }
        
        //Construct the Writer class
        try {
            Class c = Class.forName(cname);
            writer = (AbstractWriter) c.newInstance();
            writer.setRequest(request);
            writer.setResponse(response);
        } catch (Throwable t) {
            String msg = "Failed to construct the Writer class: " + cname;
            _logger.error(msg, t);
            throw new TSSException(msg, t);
        }
        
        //Give the writer the properties defined for it.
        writer.setProperties(props);
        
        //Set the Content-Type HTTP header
        response.setContentType(writer.getContentType());
        
        //Set the Content-Description HTTP header
        String cd = writer.getContentDescription(); 
        if (cd == null) cd = "tss-" + type;
        response.setHeader("Content-Description", cd);
        
        //Set date headers
        long date = System.currentTimeMillis();
        response.addDateHeader("Date", date);
        response.addDateHeader("Last-Modified", date);
        //TODO: use data publish date for Last-Modified? 
        
        //Set other HTTP headers
        String dodsServer = TSSProperties.getProperty("server.dods");
        response.setHeader("XDODS-Server", dodsServer); 
        String server = TSSProperties.getProperty("server.tss");
        response.setHeader("Server", server); 
        
        //Get the HttpServletResponse's OutputStream/Writer depending on whether we are writing binary or text.
        try {
            if (writer instanceof TextWriter) {
                PrintWriter pw = response.getWriter();
                ((TextWriter) writer).setOutput(pw);
            } else if (writer instanceof BinaryWriter) {
                ServletOutputStream sout = response.getOutputStream();
                ((BinaryWriter) writer).setOutput(sout);
            }
        } catch (IOException e) {
            String msg = "Unable to get the OutputStream/Writer from the HttpServletResponse.";
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
         
        return writer;
    }

}
