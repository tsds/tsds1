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

import org.apache.log4j.Logger;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import lasp.tss.TSSPublicException;
import lasp.tss.TimeSeriesDataset;

/**
 * Represents a hyperslab definition in the OPeNDAP constraint expression.
 * A hyperslab is an optional part of a field projection.
 * It has one of the forms:
 *   var[index]
 *   var[start:stop]
 *   var[start:stride:stop]
 * In this context the "[*]" is the hyperslab.
 * The TSS interprets a hyperslab to be applied to the time dimension. 
 * All variables will be affected.
 * 
 * @author Doug Lindholm
 */
public class HyperslabConstraint extends Constraint {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(HyperslabConstraint.class);
    
    private Range _range;

    public HyperslabConstraint(String hyperslab) {
        super(hyperslab);
        
        //remove the brackets and split on ":"
        String[] ss = hyperslab.substring(1,hyperslab.length()-1).split(":");
        int n = ss.length;
        int[] indices = new int[n];
        int i = 0;
        for (String s : ss) indices[i++] = Integer.parseInt(s);

        //Make a Range to represent the hyperslab.
        try {
            if      (n == 1) _range = new Range(indices[0],indices[0]); //start=stop
            else if (n == 2) _range = new Range(indices[0],indices[1]); //start, stop
            else if (n == 3) _range = new Range(indices[0],indices[2],indices[1]); //note reposition of stride
        } catch (InvalidRangeException e) {
            String msg = "Unable to define range for hyperslab : " + hyperslab;
            _logger.error(msg, e);
            throw new TSSPublicException(msg, e);
        }
    }

    /**
     * Subset each member variable of the TimeSeries.
     * Hyperslab applies only to the time sequence.
     */
    public void constrain(TimeSeriesDataset dataset) {
        Range range = new Range("time", _range); //name the applicable dimension
        dataset.getTimeSeries().subset(range);
    }
    
}
