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
package lasp.tss.variable;

import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Superclass of Structure and Sequence variables that contain other variables.
 * 
 * @author Doug Lindholm
 */
public abstract class CompositeVariable extends TSSVariable {
    
    /**
     * A map of names to child variables. Order will be preserved.
     */
    private LinkedHashMap<String,TSSVariable> _members = new LinkedHashMap<String,TSSVariable>();

    /**
     * CompositeVariable is currently implemented in terms of a NetCDF Group
     * instead of NetCDF Structure and Sequence which have been problematic.
     */
    private Group _ncGroup;
    
    /**
     * Construct a CompositeVariable based on a NetCDF Group.
     */
    protected CompositeVariable(CompositeVariable parent, Group ncGroup) {
        super(parent, null); //set internal ncVar to null
        _ncGroup = ncGroup;
    }
    
    
    /**
     * Override to get the name from the Group.
     */
    public String getName() {
        String name = _ncGroup.getShortName();
        return name;
    }
    
    /**
     * Return members (i.e. sub-variables) of this variable that have been projected.
     * The order will be as originally defined, not as projected.
     */
    public List<TSSVariable> getVariables() {
        List<TSSVariable> vars = new ArrayList<TSSVariable>();
        
        for (TSSVariable var : _members.values()) {
            if (var.isProjected()) vars.add(var);
        }
        
        return vars;
    }

    /**
     * Return all the member variables. 
     */
    public Collection<TSSVariable> getAllComponents() {
        return _members.values();
    }
    
    /**
     * Return a List of names of the member variables that have been projected.
     */
    public List<String> getVariableNames() {
        List<String> list = new ArrayList<String>();
        List<TSSVariable> vars = getVariables();
        for (TSSVariable var : vars) list.add(var.getName());
        return list;
    }
    
    /**
     * Return the (descendant) variable with the given name.
     * The name could be a partially complete long name.
     * The result is not limited to projected variables.
     * Return null if not found.
     */
    public TSSVariable findVariable(String name) {
        TSSVariable var = null;
        
        String vname = getName();
        if (name.equals(vname)) return this; //Found it.
        
        //If the top level components matches, this is an ancestor of the var we want.
        //Remove it from the name and move on to the kids.
        if (name.startsWith(vname+".")) {
            int index = name.indexOf(".") + 1;
            name = name.substring(index);
        }
        
        //Try all member variables. Recurse on composites.
        for (TSSVariable v : getAllComponents()) {
            if (v instanceof CompositeVariable) {
                var = ((CompositeVariable) v).findVariable(name); //recursive
                if (var != null) break; //return the first one found
            } else {
                vname = v.getName();
                if (name.equals(vname)) {
                    var = v;
                    break;
                }
            }
        }
        
        return var;
    }
    
    /**
     * Return the member variable with the given short name.
     */
    public TSSVariable getComponent(String name) {
        return _members.get(name);
    }

    /**
     * Add the given variable to this composite.
     */
    public void addComponent(TSSVariable variable) {
        String name = variable.getName();
        _members.put(name, variable);
    }
    
    /**
     * Return the number of projected variables contained in this variable.
     */
    public int getVariableCount() {
        return getVariables().size();
    }

    /**
     * Return the NetCDF Attributes from the internal NetCDF Variable,
     * ultimately from the NcML. These will be preserved in the OPeNDAP DAS output.
     */
    public List<Attribute> getAttributes() {
        return _ncGroup.getAttributes();
    }

    /**
     * Override to get attributes from NetCDF Group instead of Variable
     */
    public String getAttributeValue(String string) {
        String value = null;
        
        Attribute att = null;
        if (_ncGroup != null) att = _ncGroup.findAttribute(string);
        else att = getNetcdfVariable().findAttribute(string);
        
        if (att != null) {
            if (att.isString()) value = att.getStringValue();
            else value = att.getNumericValue().toString();
        }
        
        return value;
    }

    /**
     * Override to apply the subset the all the member Variables.
     */
    public void subset(Range range) {
        for (TSSVariable var : getAllComponents()) {
            var.subset(range);
        }
    }
    
    /**
     * Override to include values from all projected variable components.
     * Not intended for nested CompositeVariable-s. 
     */
    public double[] getValues(int timeIndex) {
        
        List<TSSVariable> vars = getVariables();
        int nvar = vars.size();  //number of vars
        
        double[] values = new double[nvar]; //assume all are scalars, one value per component variable
        int i = 0;
        
        //Get values from each projected component
        for (TSSVariable var : getVariables()) {
            double[] d = var.getValues(timeIndex);
            if (d == null) return null; //if one component is null (e.g. didn't pass filter) entire time sample is null
            values[i++] = d[0]; 
        }
        
        return values;
    }
    
    /**
     * Return the NetCDF Group that represents this variable.
     */
    public Group getNetcdfGroup() {
        return _ncGroup;
    }
    
}
