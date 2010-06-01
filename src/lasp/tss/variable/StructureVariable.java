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

import ucar.nc2.Group;

/**
 * A composite variable that represents a collection of related variables.
 * For example, a magnetic field vector.
 * 
 * @author Doug Lindholm
 */
public class StructureVariable extends CompositeVariable {

    /**
     * Construct a StructureVariable based on a NetCDF Group.
     */
    public StructureVariable(CompositeVariable parent, Group ncGroup) {
        super(parent, ncGroup);
    }

    /**
     * Return the data type for the DDS.
     */
    public String getDodsType() {
        return "Structure";
    }

    /**
     * Override superclass. Return the number of projected member variables.
     * Assume that each component of a Structure is a dependent variable and is a scalar.
     */
    public int getNumDependentVariables() {
        return getVariableCount();
    }
    
    /**
     * Get the number of bytes needed to hold the data from projected variables 
     * for one time sample.
     */
    public int getTimeSampleSizeInBytes() {
        //assume components are doubles
        int size = 8 * getVariableCount();
        return size;
    }
        
}
