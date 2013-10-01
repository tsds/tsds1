package lasp.tss.filter;

import lasp.tss.TSSPublicException;
import lasp.tss.variable.IndependentVariable;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;


public class LastSampleFilter extends SubsetFilter {
  //TODO: apply all other exclusions first
  // FilterConstraint applies SubsetFilter-s immediately

    protected Range makeRange(IndependentVariable variable) {
        Range range = variable.getRange();
        
        String name = range.getName();
        int last = range.last();
        int first = last;
        int stride = range.stride();

        try {
            range = new Range(name, first, last, stride);
        } catch (InvalidRangeException e) {
            String msg = "Unable to define range for last sample.";
            throw new TSSPublicException(msg, e);
        }
        
        return range;
    }

}
