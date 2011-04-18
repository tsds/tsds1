package lasp.tss.iosp;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import lasp.tss.TSSException;

/**
 * Read the equivalent of an ASCII tabular data file from a URL.
 */
public class AsciiWebServiceIOSP extends AsciiIOSP {
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(AsciiWebServiceIOSP.class);
    
    private URL _url;
    private BufferedReader _input;
    
    /**
     * Initialize the IOSP. Open the URL as a BufferedReader then read all the data.
     */
    protected void init() {
        String surl = getProperty("url");
        
        try {
            _url = new URL(surl);
            _input = new BufferedReader(new InputStreamReader(_url.openStream()));

        } catch (IOException e) {
            String msg = "Failed to connect to the URL: " + _url;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        
        super.init();
    }
    
    /**
     * Read line from the source URL.
     */
    protected String readLine() {
        String line;
        try {
            line = _input.readLine();
        } catch (IOException e) {
            String msg = "Unable to read line from URL: " + _url;
            _logger.error(msg, e);
            throw new TSSException(msg, e);
        }
        return line;
    }
    
    /**
     * Close the URL connection and the file that NetCDF gave us.
     */
    public void close() throws IOException {
        super.close();  //close the /dev/null that ncml gave us
        try {
            _input.close();
        } catch (IOException e) {
            String msg = "Failed to close URL connection: " + _url;
            _logger.warn(msg, e);
        }
    }
}
