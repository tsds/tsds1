package lasp.tss.filter;

import lasp.tss.variable.ScalarVariable;
import lasp.tss.variable.TSSVariable;
import lasp.tss.variable.TimeSeries;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

/**
 * Bin data over the time dimension (for now).
 * Yield the average of the data in each bin as defined by the 
 * width (in time units) of the bin. This is designed for a 
 * time series of a single scalar variable. If there are more than one
 * dependent variables, only the first one will be used.
 * The resulting time values will be bin centers.
 * This filter will also add the following variables:
 * - mean_time: The average of the time values in each bin.
 * - min: The minimum value in each bin.
 * - max: The maximum value in each bin.
 * - count: The number of samples in each bin.
 */
public class BinningFilter extends TimeSeriesFilter {
    
    //TODO: make sure this works for Vectors (spectra can wait)
    
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(BinningFilter.class);
    
    private double _binWidth;
    private double _min = Double.NaN;
    private double _max = Double.NaN;
    
    public void setArguments(String args) throws IllegalArgumentException {
        if (args != null) {
            String[] ss = args.split(",");
            
            _binWidth = Double.parseDouble(ss[0]); 
            //TODO: support iso duration format: PT30S
            
            //optional min, max args
            if (ss.length > 1) _min = Double.parseDouble(ss[1]); 
            if (ss.length > 2) _max = Double.parseDouble(ss[2]); 
            
        } else {
            String msg = "The BinningFilter requires one argument to identify the bin width and up to two optional arguments to specify min and max times.";
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public void filter(TimeSeries ts) {
        
        //Get the original data values.
        double[][] data = new double[2][];
        TSSVariable tvar = ts.getVariables().get(0);
        data[0] = tvar.getValues(); //times
        TSSVariable var = ts.getVariables().get(1); //assumes single scalar only
        data[1] = var.getValues();

        //Define min and max range of times if not entered as arguments.
        if (Double.isNaN(_min)) _min = data[0][0];
        if (Double.isNaN(_max)) _max = data[0][data[0].length - 1];
                
        //Bin the data.
        double[][] binned = bin(_binWidth, data, _min, _max);
        
        //Number of resulting bins.
        int n = binned[0].length;
        
        //replace time in the Nc Variable cache
        Variable ncvar = tvar.getNetcdfVariable();
        ncvar.getDimension(0).setLength(n); 
        ncvar.resetShape();
        int[] shape = ncvar.getShape();
        Array array = Array.factory(ncvar.getDataType(), shape, binned[0]);
        ncvar.setCachedData(array);
        
        //replace data in the Nc Variable cache
        ncvar = var.getNetcdfVariable();
        ncvar.getDimension(0).setLength(n); 
        ncvar.resetShape();
        shape = ncvar.getShape();
        array = Array.factory(ncvar.getDataType(), shape, binned[1]);
        ncvar.setCachedData(array);

        //add mean time variable
        ncvar = new Variable(null, ts.getNetcdfGroup(), null, "mean_time");
        ncvar.setDataType(DataType.DOUBLE);
        ncvar.setDimensions("time");
        array = Array.factory(ncvar.getDataType(), shape, binned[2]);
        ncvar.setCachedData(array);
        ts.addComponent(new ScalarVariable(ts, ncvar));
        
        //add minimum variable
        ncvar = new Variable(null, ts.getNetcdfGroup(), null, "min");
        ncvar.setDataType(DataType.DOUBLE);
        ncvar.setDimensions("time");
        array = Array.factory(ncvar.getDataType(), shape, binned[3]);
        ncvar.setCachedData(array);
        ts.addComponent(new ScalarVariable(ts, ncvar));
        
        //add maximum variable
        ncvar = new Variable(null, ts.getNetcdfGroup(), null, "max");
        ncvar.setDataType(DataType.DOUBLE);
        ncvar.setDimensions("time");
        array = Array.factory(ncvar.getDataType(), shape, binned[4]);
        ncvar.setCachedData(array);
        ts.addComponent(new ScalarVariable(ts, ncvar));
        
        //add count variable
        ncvar = new Variable(null, ts.getNetcdfGroup(), null, "count");
        ncvar.setDataType(DataType.DOUBLE);
        ncvar.setDimensions("time"); //TODO: assumes binning over time
        array = Array.factory(ncvar.getDataType(), shape, binned[5]);
        ncvar.setCachedData(array);
        ts.addComponent(new ScalarVariable(ts, ncvar));

    }
    
    /**
     * Bin the data based on the samples in the first array of doubles, usually time.
     * Return the reduced set of data including new arrays for:
     * - mean_time: The average of the time values in each bin.
     * - min: The minimum value in each bin.
     * - max: The maximum value in each bin.
     * - counts: the number of samples found in each bin.
     * Assumes there are no NaNs in the data.
     * Does not require samples to be sorted.
     * Bins are defined as inclusive on the lower end, exclusive on the upper.
     * New samples will represent the bin centers, not an average of the samples in that bin.
     * Empty bins will contain NaNs.
     */
    public static double[][] bin(double binWidth, double[][] data, double min, double max) {
        int nvar = 5; //number of dependent variables in the result (not counting time)
        int n = data[0].length; //number of samples in the original data
        
        int nbin = getBinCount(binWidth, min, max); //number of bins
        double[][] data2 = new double[nvar+1][nbin]; //data structure for the binned results
        
        //make ResizableDoubleArrays for each bin
        ResizableDoubleArray[] tbins = new ResizableDoubleArray[nbin]; //for times
        ResizableDoubleArray[] vbins = new ResizableDoubleArray[nbin]; //for data values
        for (int ibin=0; ibin<nbin; ibin++) {
            tbins[ibin] = new ResizableDoubleArray();
            vbins[ibin] = new ResizableDoubleArray();
        }
        
        //Put each sample into the appropriate bin
        for (int i=0; i<n; i++) {
            double sample = data[0][i];
            int ibin = (int) Math.floor((sample - min) / binWidth);
            if (ibin < 0 || ibin >= nbin) continue; //exclude samples outside desired range
            tbins[ibin].addElement(data[0][i]); 
            vbins[ibin].addElement(data[1][i]); 
        }
        
        //Gather results from the bins
        for (int ibin=0; ibin<nbin; ibin++) {
            double[] times = tbins[ibin].getElements();
            double[] values = vbins[ibin].getElements();

            //time: bin center
            data2[0][ibin] = min + binWidth * (0.5 + ibin); 
            
            //counts
            int count = times.length;
            data2[5][ibin] = count;
            
            //other stats
            if (count > 0) {
                data2[1][ibin] = StatUtils.mean(values); //mean value
                data2[2][ibin] = StatUtils.mean(times); //mean time
                data2[3][ibin] = StatUtils.min(values); //min
                data2[4][ibin] = StatUtils.max(values); //max
            } else { //empty bin
                data2[1][ibin] = Double.NaN;
                data2[2][ibin] = Double.NaN;
                data2[3][ibin] = Double.NaN;
                data2[4][ibin] = Double.NaN;
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
