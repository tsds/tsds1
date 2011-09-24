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
import ucar.unidata.io.RandomAccessFile;

public class AsciiGranuleReader extends GranuleIOSP {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(AsciiGranuleReader.class);
    
    private URL _url;
    private BufferedReader _input;
    
    protected ArrayList<String[]> _dataStrings = new ArrayList<String[]>();
    

    protected String[] parseLine(String line) {
        String[] ss = line.split(RegEx.DELIMITER);
        return ss;
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
        
        //make empty Arrays and put in data map
        List<Variable> vars = getVariables();
        int length = getLength();
        for (Variable var : vars) {
            String vname = var.getShortName();
            DataType type = var.getDataType();
            int[] shape = new int[] {length};
            Array array = Array.factory(type, shape);
            setArray(vname, array);
        }
        
        //fills Arrays with data
        int itim = 0;
        for (String[] ss : _dataStrings) {
            int ivar = 0;
            for (Variable var : vars) {
                String s = ss[ivar];
                String vname = var.getShortName();
                Array array = getArray(vname);
                if (var.getDataType().isString()) array.setObject(itim, s);
                else {
                    double d = Double.parseDouble(s);
                    array.setDouble(itim, d);
                }
                ivar++;
            }
            itim++;
        }
    }


    protected void openUrl(String surl) {
        try {
            URI uri = new URI (surl);
            if (uri.getScheme() == null) {
                //add file scheme
                surl = "file://" + surl;
            }
            
            _url = new URL(surl);
            _input = new BufferedReader(new InputStreamReader(_url.openStream()));

        } catch (Exception e) {
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
