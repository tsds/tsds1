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

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import lasp.tss.util.ServerUtils;

import org.apache.log4j.Logger;

/**
 * Provide global access to server properties.
 * Single instance constructed during Servlet init.
 * There are no "set" methods so we don't need to
 * worry about concurrency issues.
 * 
 * @author Doug Lindholm
 */
public class TSSProperties {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TSSProperties.class);
    
    private static TSSProperties _instance;
    private static ServletConfig _config;
    private static Properties _properties;
    
    
    private TSSProperties(ServletConfig config) {
        _config = config;
        
        //Get the property file location
        String propertyFile = System.getProperty("tss.config"); //Try System properties (e.g. command line with "-D")
        if (propertyFile == null) propertyFile = config.getInitParameter("config"); //Try init param in web.xml
        if (propertyFile == null) propertyFile = "tss.properties"; //Default

        //Prepend absolute path if propertyFile is relative.
        if (! propertyFile.startsWith(File.separator)) {
            propertyFile = getRealPath(propertyFile);
        }
        
        loadProperties(propertyFile);
    }
    

    public static void makeInstance(ServletConfig servletConfig) {
        if (_instance != null) {
            String msg = "An instance of TSSProperties already exists.";
            _logger.warn(msg);
        }
        _instance = new TSSProperties(servletConfig);        
    }
    
    /**
     * Read in the tss properties. 
     * Kept as static for all to use.
     */
    private void loadProperties(String fname) {
        //TODO: include servlet context attributes?
        try {
            _properties = new Properties();
            FileInputStream in = new FileInputStream(fname);
            _properties.load(in);
        } catch (Exception e) {
            String msg = "Unable to read properties file: " + fname;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
    }

    
    /**
     * Get the value of the named property.
     * Return null if it does not exist.
     */
    public static String getProperty(String name) {
        return getProperty(name, null);
    }
    
    /**
     * Get the value of the named property.
     * Return defaultValue if it does not exist.
     */
    public static String getProperty(String name, String defaultValue) {
        return _properties.getProperty(name, defaultValue);
    }

    /**
     * Make a collection of Properties that start with the given prefix followed by more.
     * Don't include the prefix in the new property names.
     */
    public static Properties getPropertiesStartingWith(String prefix) {
        //TODO: maintain order
        Properties props = new Properties();
        Properties oprops = _properties; //original properties

        for (String name : oprops.stringPropertyNames()) {
            if (name.startsWith(prefix+".")) {
                String prop = oprops.getProperty(name);
                name = name.substring(prefix.length()+1); //remove prefix+"." from the property name
                props.setProperty(name, prop);
            }
        }

        return props;
    }

    /**
     * Return the full file system pathname of the directory where the NcML lives.
     * Defined in tss.properties as "dataset.dir".
     * Default to the "datasets" directory in the root of the TSS web app.
     */
    public static String getDatasetDir() {
        String datadir = getProperty("dataset.dir", "datasets"); 

        //Prepend absolute path if dataset.dir is relative.
        if (! datadir.startsWith(File.separator)) {
            datadir = getRealPath(datadir);
        }
        //make sure dir ends with "/"
        if (! datadir.endsWith(File.separator)) datadir += File.separator;
        
        return datadir;
    }
    
    /**
     * Return a URL to the catalog for the catalog reader.
     * This could be a local file URL or remote service.
     */
    public static String getCatalogUrl() {
        //Look for the catalog.url property.
        String curl = TSSProperties.getProperty("catalog.url", "catalog.thredds");
        try {
            //If the URL is not absolute, assume it is relative to dataset.dir.
            URI uri = new URI(curl);
            if (! uri.isAbsolute()) {
                //look in dataset.dir
                String datadir = TSSProperties.getDatasetDir();
                curl = "file:" + datadir + File.separator + curl;
            }
        } catch (URISyntaxException e) {
            String msg = "Failed to parse the catalog URL: " + curl;
            _logger.warn(msg, e);
        }
        
        return curl;
    }

    public static String getCatalogHttpUrl(HttpServletRequest _request) {
        //Look for the catalog.url property.
        String curl = TSSProperties.getProperty("catalog.url", "catalog.thredds");
        try {
            //If the URL is not absolute, assume it is relative to dataset.dir.
            URI uri = new URI(curl);
            if (! uri.isAbsolute()) {
                //prepend server URL
                String baseUrl = ServerUtils.getServerUrl(_request);
                curl = baseUrl + File.separator + curl;
            }
        } catch (URISyntaxException e) {
            String msg = "Failed to parse the catalog URL: " + curl;
            _logger.warn(msg, e);
        }
        
        return curl;
    }

    /**
     * Get the file system path for the given URL path.
     */
    public static String getRealPath(String path) {
        return _config.getServletContext().getRealPath(path);
    }



}
