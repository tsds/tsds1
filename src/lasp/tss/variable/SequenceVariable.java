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
import ucar.nc2.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sequence variable represents an unlimited number of data samples.
 * It implies a functional relationship (i.e. independent and dependent variables), 
 * such as a spectrum. One variable (the independent variable) must have a 
 * dimension with the same name (a.k.a. "coordinate variable" in NetCDF).
 * It assumes that the independent variable values (e.g. wavelengths)
 * are the same for each time sample.
 * The internal data model uses a 2D NetCDF Array, for now.
 * The ncml encapsulates the Sequence components in a "group".
 * 
 * @author Doug Lindholm
 */
public class SequenceVariable extends CompositeVariable {
    
    /**
     * Construct a SequenceVariable based on a NetCDF Group and its parent.
     */
    public SequenceVariable(CompositeVariable parent, Group ncGroup) {
        super(parent, ncGroup);
    }
    
    /**
     * Return the projected variable values.
     * Variable index should vary faster than the independent variable dimension.
     * This is designed for SequenceVariables that are in the range of a TimeSeries.
     */
    public double[] getValues(int timeIndex) {
        List<TSSVariable> vars = getVariables();
        
        int nvar = vars.size();  //number of vars
        int n = getLength(); //number of samples
        
        double[] values = new double[n*nvar]; //assume all are scalars
        double[] d = null;
        
        for (int ivar=0; ivar<nvar; ivar++) {
            TSSVariable var = vars.get(ivar);
            if (var.isIndependent()) d = var.getValues();
            else d = var.getValues(timeIndex);
            if (d == null) return null;
            for (int i=0; i<n; i++) {
                int index = i*nvar + ivar;
                values[index] = d[i];
            }
        }
        
        return values;
    }
    
    /**
     * Override to apply range to each component variable
     * which is currently implemented as a 2D nc2.Variable.
     * Note, that the name of independent variable of the sequence 
     * is the same as the name of the 2nd dimension of the nc2.Variable.
     */
    public void subset(Range timeRange) {
        
        //If the Range is for the independent variable, apply it
        IndependentVariable indvar = getIndependentVariable();
        String vname = indvar.getName();
        String rname = timeRange.getName();
        if (vname.equals(rname)) indvar.subset(timeRange);
        
        //Apply to the other variables in this sequence.
        //Note that this supports our use of 2D vars for sequence members, will likely change
        for (TSSVariable var : getDependentVariables()) {
            var.subset(timeRange);
        }

    }

    /**
     * Return the number of samples, taking into account the constraints.
     * Defer to the length of the sequence's independent variable.
     */
    public int getLength() {
        int length = getIndependentVariable().getLength();
        return length;
    }

    /**
     * Return the number of dependent variables encapsulated by this variable.
     * Assumes all but one of the components are dependent.
     */
    public int getNumDependentVariables() {
        int n = getDependentVariables().size();
        return n;
    }
    
    /**
     * Get the size in bytes of a time sample including all projected variables in this Sequence.
     * Assumes all components are doubles. No nested Composites.
     */
    public int getTimeSampleSizeInBytes() {
        int size = 8 * getVariableCount() * getLength();
        return size;
    }
    
    /**
     * Return the independent variable of this Sequence.
     */
    public IndependentVariable getIndependentVariable() {
        IndependentVariable indepVariable = null;
        
        //look for the IndependentVariable among the components, only one
        for (TSSVariable var : getAllComponents()) {
            if (var instanceof IndependentVariable) {
                indepVariable = (IndependentVariable) var;
                break;
            }
        }
        
        return indepVariable;
    }
    
    /**
     * Return a list of the dependent member variables.
     */
    public List<TSSVariable> getDependentVariables() {
        List<TSSVariable> vars = new ArrayList<TSSVariable>();
        
        for (TSSVariable var : getVariables()) {
           if (var instanceof IndependentVariable) continue;
           vars.add(var);
        }
        
        return vars;
    }
    
    /**
     * Return the data type for the DDS.
     */
    public String getDodsType() {
        return "Sequence";
    }
    
}
