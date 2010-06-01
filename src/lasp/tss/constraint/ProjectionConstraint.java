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
package lasp.tss.constraint;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import lasp.tss.TSSPublicException;
import lasp.tss.TimeSeriesDataset;
import lasp.tss.util.RegEx;
import lasp.tss.variable.CompositeVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

/**
 * Represents a field projection (but not a hyperslab definition).
 * A ProjectionConstraint for a variable means it has been selected for output.
 * Experimental: There may be Functions in the projection clause. 
 *   Serve the result of this Function.
 * 
 * @author Doug Lindholm
 */
public class ProjectionConstraint extends Constraint {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(ProjectionConstraint.class);
    
    /**
     * List of full variable names that are listed in the field projection constraint.
     * This will be empty if no variables were listed, meaning that all variable are "projected".
     */
    private List<String> _projections;
    
    /**
     * Hold onto the hyperslab definition for the TimeSeriesDataset to retrieve.
     * The TimeSeriesDataset will construct a HyperslabConstraint from it and apply it.
     * This will be null if there was no hyperslab definition.
     */
    private String _hyperslab;
    
    
    public ProjectionConstraint(String expression) {
        super(expression);
        
        //Make list of variables named in the field projection clause.
        _projections = new ArrayList<String>();
       
        //Split comma separated list of variables
        String[] ss = expression.split(",");
        
        for (String s : ss) {
            //match anything that starts off looking like a variable name
            String[] matches = RegEx.match(s, RegEx.VARIABLE, ".*");
            
            //TODO: error if null?
            String vname = matches[0]; //full variable name (relative to TimeSeries Sequence)
            String slab = matches[1]; //the rest better be a hyperslab  or ""
            //error if the rest isn't a hyperslab definition
            if (slab.length() != 0 && RegEx.match(slab, RegEx.HYPERSLAB) == null) {
                String msg = "Projection constraint invalid: " + s;
                _logger.error(msg);
                throw new TSSPublicException(msg);
            }
            
            //Add projected variable name to the list. 
            _projections.add(vname);
            
            //Get hyperslab definition. Applicable to the time range only, affecting all vars.
            //The TimeSeriesDataset will get it and make a HyperslabConstraint.
            if (! slab.isEmpty()) {
                //TODO: make sure this is consistent with any other slab defn
                _hyperslab = slab;
            }
        }
        
    }
    
    /**
     * Every top level variable will be coming through here to get constrained.
     * CompositeVariable-s with be told which members have been specifically projected.
     * Multiple nested Composites have not been considered.
     */
    public void constrain(TimeSeriesDataset dataset) {
        //Set projection to false for all variables.
        TimeSeries timeSeries = dataset.getTimeSeries();
        unproject(timeSeries);
        
        //Apply projection to the chosen variables (in the projection constraint)
        //and to the relevant relatives.
        for (String name : _projections) {
            TSSVariable var = ((CompositeVariable) timeSeries).findVariable(name); 
            
            //error if null, bad var name
            if (var == null) {
                String msg = "Unable to find variable: " + name;
                _logger.error(msg);
                throw new TSSPublicException(msg);
            }
            
            //Mark descendants as projected, recursively
            project(var);
            //Mark ancestors as projected, recursively
            projectParent(var);
        }

    }

    /**
     * Tell the given variable and its descendents that they are projected.
     */
    private void project(TSSVariable variable) {
        //Mark the given variable as projected.
        variable.setProjected(true);
        
        //Mark all descendants as projected, recursively
        if (variable instanceof CompositeVariable) {
            for (TSSVariable var : ((CompositeVariable) variable).getAllComponents()) {
                project(var); //recursive
            }
        }
    }
    
    /**
     * Mark all ancestors as projected, recursively.
     */
    private void projectParent(TSSVariable variable) {
        CompositeVariable parent = variable.getParent();
        if (parent != null) {
            parent.setProjected(true);
            projectParent(parent);
        }
    }
    
    /**
     * Set projection to false for all variables, recursively.
     */
    private void unproject(TSSVariable variable) {
        //Mark the given variable as not projected.
        variable.setProjected(false);

        //Mark all descendants as not projected, recursively
        if (variable instanceof CompositeVariable) {
            for (TSSVariable var : ((CompositeVariable) variable).getAllComponents()) {
                unproject(var); //recursive
            }
        }
    }
    
    
    /**
     * Return the hyperslab expression, including the brackets.
     */
    public String getHyperslab() {
        return _hyperslab;
    }
    
    /**
     * Return full names of the projected variables.
     * If empty, project all.
     */
    public List<String> getFieldProjections() {
        return _projections;
    }

}
