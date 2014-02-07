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
package lasp.tss;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lasp.tss.writer.AbstractWriter;
import lasp.tss.writer.DatasetWriter;
import lasp.tss.writer.ErrorWriter;
import lasp.tss.writer.WriterFactory;
import lasp.tss.util.RequestWrapper;

//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OPeNDAP server to serve time series data as a Sequence.
 * 
 * @author Doug Lindholm
 */
public class TimeSeriesServer extends HttpServlet {
    
    // Initialize a logger. 
    private static final Logger _logger = LoggerFactory.getLogger(TimeSeriesServer.class);
    
    /**
     * Initialize the Servlet. Load properties.
     */
    public void init() {
        _logger.info("Initializing the TimeSeriesServer Servlet.");
        TSSProperties.makeInstance(getServletConfig());        
    }
    
    /**
     * Handle OPeNDAP request.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        TimeSeriesDataset dataset = null;
        AbstractWriter writer = null;
        
        //Get the request not including the constraints
        String path = request.getPathInfo();
        
        // Turning this off in favor of logging additional info
//        _logger.info("Handling request: "+path+"?"+request.getQueryString());
        // Log the request details
	    RequestWrapper req = new RequestWrapper((HttpServletRequest) request);
	    req.setLogInfo();
		_logger.info("");
		req.clearLogInfo();
        
        try {            
            String type = null;   //requested output type, i.e. "suffix"
            String dsname = null; //name of the data set
            
            //Get the type for special OPeNDAP requests that are not data set suffixes.
            if (path == null || path.equals("/") || path.equals("/help")) {
                type = "help";
            } else if (path.equals("/version")) {
                type = "version";
            } else {
                //Get the type of the output request from the data set suffix.
                int index = path.lastIndexOf(".");
                if (index > 0) {
                    type = path.substring(index+1);   //suffix
                    dsname = path.substring(0,index); //dataset name
                } 
            }
            
            //TODO: if url is a directory, display the catalog
            
            //Error if output type is not determined.
            if (type == null) {
                String msg = "No data set suffix is defined in the request: " + path;
                _logger.error(msg);
                throw new TSSPublicException(msg);
            }
            
            //Construct a Writer to produce the requested output.
            _logger.debug("Constructing a Writer object to handle the output type: " + type);
            writer = WriterFactory.makeWriter(type, request, response);
            if (writer == null) {
                String msg = "Unable to construct a Writer to produce the output type: " + type;
                _logger.error(msg);
                throw new TSSPublicException(msg);
            }
            
            //Construct a Dataset for Writers that need it.
            if (writer instanceof DatasetWriter) {
                _logger.debug("Constructing a Dataset object for the data set: " + dsname); 
                dataset = new TimeSeriesDataset(request);
                if (dataset == null) {
                    String msg = "Unable to construct a Dataset object for the data set: " + dsname;
                    _logger.error(msg);
                    throw new TSSPublicException(msg);
                }
                ((DatasetWriter) writer).setDataset(dataset);
            } 

            //Initialize the Writer and write the response.
            try {
                _logger.debug("Initializing the Writer: " + writer.getClass().getName());
                writer.init();
                _logger.debug("Writing the response.");
                writer.write();
            } catch (Throwable t) {
                String msg = "Unable to write the response.";
                _logger.error(msg, t);
                throw new TSSPublicException(msg, t);
            }
           
        } catch (Throwable t) {
            //Catch any Throwable and write an OPeNDAP error response.
            _logger.error("Catching Throwable.", t);

            try {
                _logger.debug("Writing Error response.");
                
                //Clear the header and anything that we tried to write.
                //It's possible that we are too late. see Javadoc, isCommitted().
                if (! response.isCommitted()) response.reset();
                
                writer = WriterFactory.makeWriter("error", request, response); //use factory to set headers and such
                ((ErrorWriter) writer).addThrowable(t);
                writer.write();
            } catch (Throwable t2) {
                // Try to stop anything else horrible from happening.
                _logger.error("Failed to write Error page.", t2);
            }
            
        } finally {
            response.setStatus(HttpServletResponse.SC_OK);
            response.flushBuffer();
            if (dataset != null) dataset.close(); //Make sure Dataset resources are closed.
            //TODO: NetcdfDataset.shutdown() to shut down FileCache?
            _logger.debug("Response is complete.");
        }
        
    }
    
}
