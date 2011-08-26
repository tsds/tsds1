package lasp.tss.iosp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.util.RegEx;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

public class AsciiGranuleReader extends GranuleIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(AsciiGranuleReader.class);
    
    private URL _url;
    private BufferedReader _input;
    
    private ArrayList<String[]> _dataStrings = new ArrayList<String[]>();
    
    protected Array getData(Variable var) {
        DataType type = var.getDataType();
        int[] shape = var.getShape();

        String col = getVariableXmlAttribute(var.getShortName(), "column");
        
        String[] scols = col.split(RegEx.DELIMITER);
        int ncol = scols.length;
        int[] cols = new int[ncol];
        int icol = 0;
        for (String s : scols) {
            cols[icol++] = Integer.parseInt(s) - 1; //first column is 1, indexed as 0
        }
        
        int n = shape[0];
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
        String surl = getProperty("url");
        if (surl != null) openUrl(surl);
        
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
            
            String[] ss = line.split(RegEx.DELIMITER);
            _dataStrings.add(ss);
            
            line = readLine();
        }
        
    }

    private void openUrl(String surl) {
        try {
            _url = new URL(surl);
            _input = new BufferedReader(new InputStreamReader(_url.openStream()));

        } catch (IOException e) {
            String msg = "Failed to connect to the URL: " + _url;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
    }

    /**
     * Return the number of time samples.
     * If not defined, get from the size of the data.
     */
    protected int getLength() {
        int length = super.getLength(); //from ncml def
        
        if (length < 0) { //not yet defined
            length = _dataStrings.size();
        }
        
        return length;
    }
    

    /**
     * Determine if the given line is a comment.
     */
    private boolean isComment(String line) {
        String comment = getProperty("commentCharacter");
        boolean b = (comment != null) && (line.startsWith(comment));
        return b;
    }

    /**
     * Skip the given number of lines in the data file (e.g. header).
     */
    private void skipLines(int n) {
        for (int i=0; i<n; i++) readLine();
    }

    /**
     * Return the next line in the file. Will be null if there are none left.
     */
    protected String readLine() {
        RandomAccessFile file = getFile();
        String line;
        try {
            if (_input != null) line = _input.readLine();
            else line = file.readLine();
        } catch (IOException e) {
            String msg = "Unable to read line from file: " + file.getLocation();
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        return line;
    }
    
    public void close() throws IOException {
        super.close();
        
        //if we are managing our own URL, close the reader
        if (_input != null) _input.close();
    }
}
