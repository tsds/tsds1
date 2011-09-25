package lasp.tss.iosp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        _nvar = getVariableCount();
    }

    @Override
    protected String[] parseRecord(String record) {
        String[] strings = null;
        
        Matcher m = _pattern.matcher(record);
        if (m.matches()) {
            strings = new String[_nvar];
            for (int i=0; i<_nvar; i++) {
                strings[i] = m.group(i+1);
            }
        }
        
        return strings;
    }

}
