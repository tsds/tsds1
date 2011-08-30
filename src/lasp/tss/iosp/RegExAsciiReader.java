package lasp.tss.iosp;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ucar.nc2.Variable;

/**
 * Read an ASCII granule one line at a time using a regular expression
 * to extract data values. 
 */
public class RegExAsciiReader extends AsciiGranuleReader {
    
    private Pattern _pattern;
    private int _nvar;
    
    
    @Override
    protected void init() {
        super.init();
        
        String regex = getProperty("regex");
        _pattern = Pattern.compile(regex);
        
        List<String> vars = getVariableNames();
        _nvar = vars.size();
    }

    protected String[] parseLine(String line) {
        String[] strings = null;
        
        Matcher m = _pattern.matcher(line);
        if (m.matches()) {
            strings = new String[_nvar];
            for (int i=0; i<_nvar; i++) {
                strings[i] = m.group(i+1);
            }
        }
        
        return strings;
    }

}
