package lasp.tss.iosp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.util.RegEx;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

/**
 * Class for reading data from ASCII sources.
 * Parsing assumes one variable per column.
 * Subclasses may override parseLine to support more complex formatting.
 * Assumes one line per time sample.
 */
public class AsciiGranuleReader extends GranuleIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(AsciiGranuleReader.class);
    
    private BufferedReader _reader;

    protected ArrayList<String[]> _dataStrings = new ArrayList<String[]>();

    
    @Override
    protected void readAllData() {
        _reader = openReader();
        List<String> records = readRecords();
        
        for (String record : records) {
            String[] ss = parseRecord(record);
            if (ss != null) _dataStrings.add(ss);
        }
    }
    
    /**
     * Return each variable value as a String.
     */
    protected String[] parseRecord(String record) {
        String delim = getDelimiter();
        String[] ss = record.split(delim);
        return ss;
    }
       
    protected Array getData(Variable var) {
        int ivar = getVariables().indexOf(var);
        DataType type = var.getDataType();
        int[] shape = var.getShape();
        Array array = Array.factory(type, shape);
        
        //fills Arrays with data
        int itim = 0;
        for (String[] ss : _dataStrings) {
            String s = ss[ivar];
            if (var.getDataType().isString()) {
                array.setObject(itim++, s);
            } else {
                double d = Double.parseDouble(s);
                array.setDouble(itim++, d);
            }
        }
        
        return array;
    }
    
    protected String getDelimiter() {
        String delimiter = null;
        
        String s = getProperty("delimiter");
        if (s != null) delimiter = s;
        else delimiter = RegEx.DELIMITER;
        
        return delimiter;
    }
    

    /**
     * Read the data source into a List of String records.
     * Each record represents a single time sample.
     * This reader assumes one record per line.
     */
    private List<String> readRecords() {
        List<String> records = new ArrayList<String>();
        
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
                continue;
            }

            records.add(line);
            
            line = readLine();
        }

        return records;
    }

    protected BufferedReader openReader() {
        BufferedReader reader = null;
        
        String url = getProperty("url");
        if (url != null) reader = openUrl(url);
        
        return reader;
    }
    
    protected BufferedReader openUrl(String surl) {
        BufferedReader reader = null;
        URL url = null;
        
        try {
            URI uri = new URI (surl);
            if (uri.getScheme() == null) {
                //add file scheme
                surl = "file://" + surl;
            }
            
            url = new URL(surl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

        } catch (Exception e) {
            String msg = "Failed to open the URL: " + url;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        
        return reader;
    }

    /**
     * Return the number of time samples.
     * If not defined, get from the size of the data.
     */
    protected int getLength() {
        int length = super.getLength(); //from ncml def
        
        if (length <= 0) { //not yet defined
            length = _dataStrings.size();
        }
        
        return length;
    }
    

    /**
     * Determine if the given line is a comment.
     */
    protected boolean isComment(String line) {
        String comment = getProperty("commentCharacter");
        boolean b = (comment != null) && (line.startsWith(comment));
        return b;
    }

    /**
     * Skip the given number of lines in the data file (e.g. header).
     */
    protected void skipLines(int n) {
        for (int i=0; i<n; i++) readLine();
    }

    /**
     * Return the next line. Will be null if there are none left.
     * If there is an input BufferedReader defined, use it. Else
     * use the RandomAccessFile that Netcdf gave us.
     */
    protected String readLine() {
        String line;
        
        try {
            if (_reader != null) line = _reader.readLine();
            else line = getFile().readLine();
        } catch (IOException e) {
            String msg = "Unable to read line.";
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        
        return line;
    }
    
    public void close() throws IOException {
        super.close();
        
        //if we are managing our own reader, close it
        if (_reader != null) _reader.close();
    }
}
