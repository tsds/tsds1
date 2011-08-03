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

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TSSProperties;
import lasp.tss.util.CatalogUtils;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;

/**
 * Part of DAP spec. This will be the response if there is no "dataset" in the request
 * or it is "help".
 * 
 * @author Doug Lindholm
 */
public class HelpWriter extends HtmlWriter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(HelpWriter.class);
    
    /**
     * THREDDS catalog
     */
    private InvCatalogImpl _catalog;
    
    /**
     * Initialize the Writer.
     */
    public void init() {
        //read the THREDDS catalog
        readCatalog(); 
    }
    
    /**
     * Write information to help the user.
     */
    public void writeContent() {
       
        println("<H1>Time Series Server</H1>");
        
        //write general usage instructions
        writeUsage();
        
        //present the available datasets from the THREDDS catalog
        if (_catalog != null) {
            println("<H2><A name=\"dataset\">Available Datasets</A></H2>");
            println("<blockquote>");
            writeCatalog();
            println("</blockquote>");
        }
        
        //present the suffix (output) options
        println("<H2><A name=\"suffix\">Output Options (<i>suffix</i>)</A></H2>");
        println("<blockquote>");
        writeOptions("writer");
        println("</blockquote>");
        
        //present the filter options
        println("<H2><A name=\"filter\">Filter Options</A></H2>");
        println("<blockquote>");
        writeOptions("filter");
        println("</blockquote>");

    }

    /**
     * Write basic usage information.
     */
    protected void writeUsage() {
        String burl = getServerUrl();
        println("<blockquote>");
        println("<b><i>Usage:</i></b> " + burl + "/<i>dataset</i>.<i>suffix</i>?<i>projection</i>&<i>selection</i>&<i>filter</i><br/><br/>");
        println("<b><i>dataset:</i></b> Name of dataset (see <A href=\"#dataset\">Available Datasets</A>)<br/>");
        println("<b><i>suffix:</i></b> Type of output (see <A href=\"#suffix\">Output Options</A>)<br/>");
        
        StringBuilder proj_usage = new StringBuilder("<b><i>projection:</i></b> List of variables to return");
        proj_usage.append(" with optional <i>hyperslab</i> (index subset) definitions. Default to all.<br/>");
        proj_usage.append(" The Dataset Descriptor Structure (DDS) will describe the variables for each dataset.");
        proj_usage.append(" Use the <i>dds</i> suffix to get a dataset's DDS. <br/>");
        println(proj_usage.toString());
        
        println("<b><i>selection:</i></b> Zero or more relative constraints on a variable (e.g. time>=2010-01-01). Each must be preceded by a '&'.<br/>");
        println("<b><i>filter:</i></b> Zero or more filters to be applied to the data (see <A href=\"#filter\">Filter Options</A>). Each must be preceded by a '&'.<br/>");
        println("</blockquote>");
    }
    
    /**
     * Write the list of data sets in the catalog
     * with links to their "html" output. 
     * Use nested output for composite datasets and catalogRefs.
     */
    protected void writeCatalog() {
        List<InvDataset> datasets = _catalog.getDatasets();
        String cat = getCatalogDatasetsAsString(datasets);
        println(cat);
    }
    
    /**
     * Return an HTML representation of the given list of catalog datasets.
     * Show the name with a link to the html form. Include description if defined.
     * Recursively indent dataset collections, including references to external catalogs.
     */
    private String getCatalogDatasetsAsString(List<InvDataset> datasets) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<dl>");
        for (InvDataset ds : datasets) {
            String name = ds.getName();
            
            if (ds.hasNestedDatasets()) {
                sb.append("<h3>" + name + "</h3>\n");
                List<InvDataset> dss = ds.getDatasets();
                String s = getCatalogDatasetsAsString(dss);
                sb.append("<blockquote>" + s + "</blockquote>");
            } else {
                //add link for each dataset
                for (InvAccess access : ds.getAccess()) {
                    String url = access.getStandardUrlName();
                    String summary = ds.getDocumentation("summary");
                    sb.append("<dt><b><i><a href=\"" + url + ".html\">" + name + "</a></i></b></dt>\n");
                    if (summary != null) sb.append("<dd>" + summary + "</dd>\n");
                }
            }
        }
        sb.append("</dl>\n");
        
        return sb.toString();
    }

    /**
     * Present the options for the given type (writer or filter).
     * This will include all options in tss.properties that dave a description.
     */
    protected void writeOptions(String type) {
        Properties props = TSSProperties.getPropertiesStartingWith(type);
        
        //Make unique set of supported suffixes
        Set<String> names = new LinkedHashSet<String>();
        for (String name : props.stringPropertyNames()) {
            String[] ss = name.split("\\.");
            names.add(ss[0]);
        }
        
        println("<dl>");
        
        //print each suffix that has a description, 
        for (String name : names) {
            String desc = TSSProperties.getProperty(type+"."+name+".description");
            if (desc != null) {
                println("<dt><b><i>" + name + "</i></b></dt> <dd>" + desc + "</dd>");
            }
        }

        println("</dl>");
    }

    /**
     * Read the top level THREDDS catalog file.
     * Assumes that it exists as "catalog.thredds" in the dataset.dir.
     */
    private void readCatalog() {
        //Get the full URL to the catalog file.
        String curl = getServerUrl() + "/catalog.thredds";
        
        _catalog = CatalogUtils.readCatalog(curl);
    }
}
