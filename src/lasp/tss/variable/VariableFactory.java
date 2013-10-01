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

import java.util.List;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

/**
 * Class to construct TSSVariables.
 * 
 * @author Doug Lindholm
 */
public class VariableFactory {
    
    /**
     * Make the TimeSeries variable from the nc2.Group definition.
     * It is the top level variable so it has no parent.
     */
    public static TimeSeries makeTimeSeries(Group ncGroup) {
        TimeSeries ts = (TimeSeries) makeVariable(null, ncGroup);
        return ts;
    }
    
    /**
     * Make a CompositeVariable from the nc2.Group definition.
     */
    public static TSSVariable makeVariable(CompositeVariable parent, Group ncGroup) {
        CompositeVariable cvar = null;
        
        //If the Group has a dimension, then it is a Sequence.
        List<Dimension> dims = ncGroup.getDimensions();
        if (dims.isEmpty()) {
            cvar = new StructureVariable(parent, ncGroup); 
        } else {
            if (parent == null) cvar = new TimeSeries(ncGroup);
            else cvar = new SequenceVariable(parent, ncGroup);            
        }

        //Make component variables
        for (Variable ncvar : ncGroup.getVariables()) {
            TSSVariable var = makeVariable(cvar, ncvar);
            cvar.addComponent(var);
        }
        
        //Make nested CompositeVariable-s from groups
        List<Group> groups = ncGroup.getGroups();
        for (Group group : groups) {
            TSSVariable var = VariableFactory.makeVariable(cvar, group); //recursive
            cvar.addComponent(var);
        }
        
        return cvar;
    }
    
    /**
     * Factory method to make the appropriate subclass of TSSVariable
     * for the given NetCDF Variable.
     */
    public static TSSVariable makeVariable(CompositeVariable parent, Variable ncVariable) {
        
        String vname = ncVariable.getShortName();
        DataType type = ncVariable.getDataType();
        
        //make the appropriate subclass of TSSVariable
        TSSVariable var = null;
        if (vname.equals("time")) var = new TimeVariable(parent, ncVariable);
        else if (type.equals(DataType.STRING)) var = new StringVariable(parent, ncVariable);
        else if (ncVariable.isCoordinateVariable()) var = new IndependentVariable(parent, ncVariable); 
        else var = new ScalarVariable(parent, ncVariable);
        
        return var;
    }

}
