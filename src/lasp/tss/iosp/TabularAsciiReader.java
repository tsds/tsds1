package lasp.tss.iosp;

import lasp.tss.util.RegEx;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

public class TabularAsciiReader extends AsciiGranuleReader {

    /*
     * TODO: reuse readAllData and getData, override parseLine only 
     * Don't use _dataStrings from superclass
     */
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(TabularAsciiReader.class);
    
    
    protected Array getData(Variable var) {
        DataType type = var.getDataType();
        int[] shape = var.getShape();
        int n = shape[0];

        String col = getVariableXmlAttribute(var.getShortName(), "column");
        
        String[] scols = col.split(RegEx.DELIMITER);
        int ncol = scols.length;
        int[] cols = new int[ncol];
        int icol = 0;
        for (String s : scols) {
            cols[icol++] = Integer.parseInt(s) - 1; //first column is 1, indexed as 0
        }
        
        Array array = Array.factory(type, shape);
        for (int i=0; i<n; i++) {
            String[] ss = _dataStrings.get(i); //ith row of data as array of strings
            String s = ss[cols[0]]; //data as string from desired column
            
            if (var.getDataType().isString()) {
                if (ncol > 1) { //combine multiple columns into one string, e.g. formatted time
                    StringBuilder sb = new StringBuilder(s);
                    for (int ic=1; ic<ncol; ic++) {
                        sb.append(" ").append(ss[cols[ic]]);
                    }
                    s = sb.toString();
                }
                array.setObject(i, s);
            } else {
                double d = Double.parseDouble(s);
                array.setDouble(i, d);
            }
        }
        
        return array;   
    }
    
    @Override
    protected void readAllData() {
        //If the NcML defines a url to use instead of the "location"
        //manage the reader
        String url = getURL();
        openUrl(url);
        
        //skip header
        String headerLength = getProperty("headerLength");
        if (headerLength != null) {
            int linesToSkip = Integer.parseInt(headerLength);
            skipLines(linesToSkip);
        }
        
        //Start reading one line at a time.
        String line = readLine();
        while (line != null) {
            line = line.trim();
            
            //skip empty or commented line
            if (line.isEmpty() || isComment(line)) {
                line = readLine();
                if (line == null) break;
                else continue;
            }

            String[] ss = parseLine(line);
            if (ss != null) _dataStrings.add(ss);
            
            line = readLine();
        }
    }

}
