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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lasp.tss.constraint.Constraint;
import lasp.tss.constraint.ConstraintExpression;
import lasp.tss.util.NestedText;
import lasp.tss.variable.CompositeVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;
import lasp.tss.variable.TimeVariable;
import lasp.tss.variable.VariableFactory;

import org.apache.log4j.Logger;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;

/**
 * Dataset for time series data with an NcML descriptor.
 * The NcML file describes the data structure and adds metadata. 
 * The TimeSeriesServer currently serves time series with data of three types:
 * - Scalar: One or more values for each time sample
 * - Structure: Multiple related components (e.g. vector). 
 * - Sequence: One or more variables as a function of an independent variable (e.g. spectrum).
 * 
 * @author Doug Lindholm
 */
public class TimeSeriesDataset {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TimeSeriesDataset.class);
    
    private HttpServletRequest _request;
    private NetcdfDataset _ncDataset;
    private TimeSeries _timeSeries;
    private ConstraintExpression _constraintExpression;
    private String _dds;
    private String _das;
    
  //===========================================================================
    
    /**
     * Construct a TimeSeriesDataset given the information from the HTTP request.
     */
    public TimeSeriesDataset(HttpServletRequest request) {
        _request = request;
        
        try {
            //Read the NcML file (not the data) to construct a NetCDF Dataset.
            _ncDataset = readNcML();
            if (_ncDataset == null) {
                String msg = "Unable to construct a NetCDF Dataset from the NcML.";
                _logger.error(msg);
                throw new TSSException(msg);
            }
            
            //Construct the variables that make up this time series (still no data).
            _timeSeries = constructTimeSeries();
            if (_timeSeries == null) {
                String msg = "Unable to construct the TimeSeries Sequence.";
                _logger.error(msg);
                throw new TSSException(msg);
            }

            //Parse and apply the constraint expression if we have one. (maybe read some indep var data)
            applyConstraints();

        } catch (Exception e) {
            //Clean up resources. The server would typically handle it in a finally block, but not if this Dataset is null.
            close();
            String msg = "Failed to construct the dataset: " + getName();
            throw new TSSException(msg, e);
        }
    }
    
  //------------------------------------------------------------------------

    /**
     * Read the NcML file to create a NetcdfDataset.
     */
    private NetcdfDataset readNcML() { 
        String url = getNcmlURL();
        return createNetcdfDataset(url);  
    }

    /**
     * Construct the NcML file URL.
     */
    private String getNcmlURL() {
        String path = _request.getPathInfo(); 
        int index = path.lastIndexOf(".");
        
        String datadir = TSSProperties.getDatasetDir();
        String filename = datadir + path.substring(0, index) + ".ncml";
        
        String url = "file:"+filename;
        return url;
    }

    private NetcdfDataset createNetcdfDataset(String ncmlURL) {
        NetcdfDataset dataset = null;
        
        try {
            CancelTask cancelTask = null; //not used
            dataset = NcMLReader.readNcML(ncmlURL, cancelTask);
        } catch(Exception e) { 
            String msg = "Unable to construct the Dataset: " + getName();
            throw new TSSPublicException(msg, e);
        }
        
        return dataset;
    }
    
    /**
     * Construct the TimeSeries SequenceVariable and all its components.
     * No data should be read.
     */
    private TimeSeries constructTimeSeries() {
        Group rootGroup = _ncDataset.getRootGroup();
        TimeSeries timeSeries = VariableFactory.makeTimeSeries(rootGroup);
        return timeSeries;
    }     
    
  //------------------------------------------------------------------------
    
    /**
     * Parse the constraint expression (everything after the "?" in the request URL).
     * There are three types of constraints:
     * 1) ProjectionConstraint: 
     *    Optional list of variable names to be served. 
     *    May also have a hyperslab (index range) definition which will be treated as a unique HyperslabConstraint.
     *    Immediately after the "?" and up to the first "&".
     * 2) SelectionConstraint:
     *    Compares a variable's values for acceptance by applying operators: >, >=, <, <=, =, ~= (pattern match)
     *    Also supports non-standard "~" for "almost equals." Currently nearest neighbor.
     * 3) FilterConstraint:
     *    Looks like a function with (optionally) arguments in "()".
     *    
     * Each constraint will be applied to the TimeSeries Sequence and all its components.
     */
    private void applyConstraints() {

        String ce = _request.getQueryString();
        _constraintExpression = new ConstraintExpression(ce); 
        
        for (Constraint constraint : _constraintExpression.getConstraints()) { //will be empty if no CE was defined
            constraint.constrain(this);
        }
    }
  

    /**
     * Return the variable representing the time.
     */
    public TimeVariable getTimeVariable() {
        TimeVariable var = (TimeVariable) _timeSeries.getIndependentVariable();
        return var;
    }

    /**
     * Return the top level TimeSeries SequenceVariable.
     */
    public TimeSeries getTimeSeries() {
        return _timeSeries;
    }
    
    /**
     * Return the constrained time samples.
     */
    public double[] getTimes() {
        return getTimeVariable().getValues();
    }

    /**
     * Return the number of time samples that meet the time constraints.
     */
    public int getLength() {
        int length = getTimeSeries().getLength();
        return length;
    }
    
    /**
     * Return the name of the dataset. It is the portion of the request URL
     * that follows the servlet name up to the suffix.
     */
    public String getName() {
        String path = _request.getPathInfo();
        int index = path.lastIndexOf(".");
        String name = path.substring(1, index); //don't include leading "/"
        return name;
    }
    
    /**
     * Return the "global" attributes of this dataset.
     */
    public List<Attribute> getAttributes() {
        return _ncDataset.getRootGroup().getAttributes();
    }

    /**
     * Return a String representing the URL of the request.
     */
    public String getRequestUrl() {
        String url = _request.getRequestURL().toString();
        return url;
    }

    /**
     * Return the constraint expression portion of the request URL.
     */
    public String getConstraintExpression() {
        String ce = _request.getQueryString();
        return ce;
    }
    
    /**
     * Try to close the NetcdfDataset.
     * Ignore Exceptions.
     */
    public void close() {
        try {if (_ncDataset != null) _ncDataset.close();} catch(Exception e) {} 
    }
    
    /**
     * Use the DDS as the String representation of the Dataset.
     */
    public String toString() {
        return getDDS();
    }
    
  //----- DDS and DAS --------------------
    
    /**
     * Construct the Dataset Descriptor Structure as a String.
     */
    public String getDDS() {
        if (_dds == null) { 
            NestedText text = new NestedText();
            
            //Start the Dataset definition
            text.append("Dataset {");
            
            //Write variable structure for each projected variable.
            //Composite components will be handled recursively.
            TimeSeries ts = getTimeSeries();
            appendVariableToDds(text, ts);
            
            //Close the Dataset block
            text.append("} " + getName() + ";");
            
            _dds = text.toString();
        }

        return _dds;
    }
    
    private void appendVariableToDds(NestedText text, TSSVariable variable) {
        String type = variable.getDodsType();
        String name = variable.getName();
        
        //Make new block for CompositeVariables
        if (variable instanceof CompositeVariable) {
            text.append(type + " {");
            List<TSSVariable> vars = ((CompositeVariable) variable).getVariables(); 
            for (TSSVariable var : vars) {
                appendVariableToDds(text, var); //recursive
            }
            text.append("} " + name + ";");
        } else { //Scalar variable
            text.append(type + " " + name + ";");
        }
    }
    

    /**
     * Construct the Dataset Attribute Structure as a String.
     */
    public String getDAS() {
        if (_das == null) { 
            NestedText text = new NestedText();
            
            //Start attributes
            text.append("attributes {");
            
            //Write variable attributes for each projected variable.
            //Composite components will be handled recursively.
            TimeSeries ts = getTimeSeries();
            appendVariableToDas(text, ts);
            
            //Close the attributes block
            text.append("}");
            
            _das = text.toString();
        }   
        
        return _das;
    }
    
    private void appendVariableToDas(NestedText text, TSSVariable variable) {
        String name = variable.getName();
        
        text.append(name + " {");
        
        List<Attribute> atts = variable.getAttributes();
        //If this is a Sequence Variable, add a "length" attribute: number of time samples 
        if (variable.isSequence()) {
            int n = variable.getLength();
            Attribute att = new Attribute("length", n);
            atts.add(att);
        }
        appendAttributesToDas(text, atts);
        
        //Deal with components of Structures and Sequences
        if (variable instanceof CompositeVariable) {
            List<TSSVariable> vars = ((CompositeVariable) variable).getVariables(); 
            for (TSSVariable var : vars) {
                appendVariableToDas(text, var); //recursive
            }
        }

        text.append("}");
    }
    
    /**
     * Add a line to the DAS for each Attribute.
     */
    private void appendAttributesToDas(NestedText text, List<Attribute> attributes) {

        for (Attribute att : attributes) {
            String name = att.getName();
            String type = null;
            String value = null;
            
            DataType ncType = att.getDataType();
            if (ncType.isString()) {
                type = "string";
                String s = att.getStringValue();
                value = "\"" + s + "\""; //put quotes around string
            } else {
                //supports only int and double, for now
                if (ncType.isIntegral()) type = "int32";
                else type = "float64";
                Number num = att.getNumericValue();
                value = num.toString();
            }
            
            text.append(type +" "+ name +" "+ value +";");
        }
    }

}
