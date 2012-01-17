package lasp.tss.iosp;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

public class BinGranuleReader extends GranuleIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(BinGranuleReader.class);
    
    @Override
    protected void readAllData() {

        //Determine the byte order of the binary data.
        String endian = getProperty("byteOrder");
        if (endian == null) endian = "big"; //default to big endian
        int bo = -1;
        if (endian.toLowerCase().startsWith("big")) bo = RandomAccessFile.BIG_ENDIAN;
        else if (endian.toLowerCase().startsWith("little")) bo = RandomAccessFile.LITTLE_ENDIAN;
        else _logger.warn("Unable to parse byteOrder: " + endian + ". Defaulting to big-endian.");
        RandomAccessFile raFile = getFile();
        raFile.order(bo);
        
        int ntim = getLength();
        List<Variable> vars = getNetcdfFile().getRootGroup().getVariables();
        int nvar = vars.size();
        int n = ntim * nvar;
        double[] blob = new double[n];

        try {
            raFile.readDouble(blob, 0, n);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        double[][] data = new double[nvar][ntim];
        int ii = 0;
        for (int itim=0; itim<ntim; itim++) {
            for (int ivar=0; ivar<nvar; ivar++) {
                data[ivar][itim] = blob[ii++];
            }
        }
        

        int[] shape = new int[] {ntim}; //var.getShape(); not defined yet
        
        int ivar = 0;
        for (Variable var : vars) {
            String vname = var.getShortName();
            Array array = Array.factory(DataType.DOUBLE, shape, data[ivar++]);
            setArray(vname, array);
        }
        
    }
    
    @Override
    protected int getLength() {
        int length = super.getLength();
        
        //If the "length" attribute is not defined, compute length.
        if (length <= 0) {
            try {
                int nvar = getNetcdfFile().getRootGroup().getVariables().size();
                length = (int) (getFile().length() / nvar / 8);
            } catch (IOException e) {
                _logger.warn("Failed to get length from data file.", e);
                //TODO: error? throw exception?
            }
        }
        
        return length;
    }

}
