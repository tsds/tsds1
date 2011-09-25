package lasp.tss.iosp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;


/**
 * Read the results of executing a system command as defined in the NcML.
 */
public class CommandReader extends AsciiGranuleReader {
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(CommandReader.class);
    
    
    protected BufferedReader openReader() {
        BufferedReader reader = null;
        
        String cmd = getProperty("command");
        if (cmd != null) reader = execCommand(cmd);
        //TODO: else error
        
        return reader;
    }
    
    private BufferedReader execCommand(String command) {
        BufferedReader reader = null;
        
        try {
            String[] args = command.split("\\s"); //split command and args on white space
            Runtime run = Runtime.getRuntime();
            Process process = run.exec(args);
            process.waitFor(); //wait till command completes //TODO: make cancelable
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        } catch (Exception e) {
            _logger.error("Failed to execute command: "+command, e);
            e.printStackTrace();
        } 
        
        return reader;
    }

}
