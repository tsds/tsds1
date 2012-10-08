package lasp.tss.filter;

import lasp.tss.variable.ScalarVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

/**
 * Bin data over the time dimension (for now).
 * Yield the average of the data in each bin as defined by the 
 * width (in time units) of the bin.
 * This filter will also add a new count variable to represent
 * the number of samples in each bin.
 */
public class BinningFilter extends TimeSeriesFilter {
    
    //TODO: make sure this works for Vectors (spectra can wait)
    
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(BinningFilter.class);
    
    private double _binWidth;
    
    public void setArguments(String args) throws IllegalArgumentException {
        if (args != null) {
            _binWidth = Double.parseDouble(args); 
            //TODO: support iso duration format: PT30S
            
            //TODO: optional min, max args
            
        } else {
            String msg = "The BinningFilter requires one argument to identify the bin width.";
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public void filter(TimeSeries ts) {
        //assumes scalars only, for now
        
        int nvar = ts.getVariableCount();
        int n = ts.getLength();

        double[][] data2 = new double[nvar][n];
        
        int ivar = 0;
        for (TSSVariable var : ts.getVariables()) {
            data2[ivar] = var.getValues();
            ivar++;
        }

        double[][] binned = bin(_binWidth, data2);
        
        int n2 = binned[0].length;
        
        //replace data in the Nc Variable caches
        int[] shape = null;
        ivar = 0;
        for (TSSVariable tssvar : ts.getVariables()) {
            Variable ncvar = tssvar.getNetcdfVariable();
            ncvar.getDimension(0).setLength(n2); 
            ncvar.resetShape();
            shape = ncvar.getShape();
            Array array = Array.factory(ncvar.getDataType(), shape, binned[ivar]);
            ncvar.setCachedData(array);
            ivar++;
        }
        //add count variable
        Variable ncvar = new Variable(null, ts.getNetcdfGroup(), null, "count");
        ncvar.setDataType(DataType.DOUBLE);
        ncvar.setDimensions("time"); //TODO: assumes binning over time

        Array array = Array.factory(ncvar.getDataType(), shape, binned[ivar]);
        ncvar.setCachedData(array);
        
        ScalarVariable cntvar = new ScalarVariable(ts, ncvar);
        ts.addComponent(cntvar);

    }
    
    /**
     * Bin the data based on the samples in the first array of doubles, usually time.
     * Return the reduced set of data including a new array for counts: the number of samples found in each bin.
     * Assumes there are no NaNs in the data.
     * Does not require samples to be sorted.
     * Bins are defined as inclusive on the lower end, exclusive on the upper.
     * New samples will represent the bin centers, not an average of the samples in that bin.
     * Empty bins will contain NaNs.
     */
    public static double[][] bin(double binWidth, double[][] data, double min, double max) {
        int nvar = data.length; //counting the one we are binning over but not the new count variable
        int n = data[0].length; //number of samples in the original data
        
        int nbin = getBinCount(binWidth, min, max); //number of bins
        double[][] data2 = new double[nvar+1][nbin]; //data structure for the binned results
        
        //TODO: predefine data2 with bin midpoints and NaNs (empty bins)
        for (int ibin=0; ibin<nbin; ibin++) {
            data2[0][ibin] = min + binWidth * (0.5 + ibin); //bin center
            for (int ivar=1; ivar<nvar; ivar++) data2[ivar][ibin] = Double.NaN;
            data2[nvar][ibin] = 0; //counts
        }
        
        for (int i=0; i<n; i++) {
            double sample = data[0][i];
            int ibin = (int) Math.floor((sample - min) / binWidth);
            if (ibin < 0 || ibin >= nbin) continue;
            
            for (int ivar=1; ivar<nvar; ivar++) {
                double d = data2[ivar][ibin];
                if (Double.isNaN(d)) data2[ivar][ibin] = data[ivar][i];
                else data2[ivar][ibin] += data[ivar][i];
            }
            
            data2[nvar][ibin] += 1; //increment counter for this bin
        }
        
        //Do averages
        for (int ibin=0; ibin<nbin; ibin++) {
            for (int ivar=1; ivar<nvar; ivar++) {
                data2[ivar][ibin] /= data2[nvar][ibin];
            }
        }
        
        return data2;
    }
    
    public static double[][] bin(double binWidth, double[][] data) {
        return bin(binWidth, data, data[0][0], data[0][data[0].length -1]);
    }


    public static int getBinCount(double binWidth, double min, double max) {
        return (int) Math.floor((max - min) / binWidth) + 1;
    }

    //assumes sorted
    public static int getBinCount(double binWidth, double[] data) {
        return getBinCount(binWidth, data[0], data[data.length-1]);
    }

}
