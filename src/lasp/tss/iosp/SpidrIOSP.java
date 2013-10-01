package lasp.tss.iosp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lasp.tss.util.RegEx;

import org.apache.log4j.Logger;

import ucar.nc2.units.DateUnit;

/**
 * Read the equivalent of an ASCII tabular data file from a URL.
 */
public class SpidrIOSP extends ColumnarAsciiReader {
    //e.g. http://spidr.ngdc.noaa.gov/spidr/servlet/GetData?format=csv&param=index_ssn&dateFrom=19000101&dateTo=21000101
    
    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(SpidrIOSP.class);
    
    @Override
    protected String getURL() {
        //get time range from query
        String query = getQuery();
        String dateFrom = getProperty("defaultFrom");
        String dateTo   = getProperty("defaultTo");
        String[] expressions = query.split("&");
        for (String s : expressions) {
            //TODO: handle exclusivity?
            if (s.startsWith("time>")) {
                String[] ss = RegEx.match(s, RegEx.VARIABLE, RegEx.SELECTION_OPERATOR, ".+");
                dateFrom = formatTime(ss[2]);
            }
            if (s.startsWith("time<")) {
                String[] ss = RegEx.match(s, RegEx.VARIABLE, RegEx.SELECTION_OPERATOR, ".+");
                dateTo = formatTime(ss[2]);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(getProperty("baseURL"));
        
        String param = getProperty("param");
        sb.append("&").append("param=").append(param);

        sb.append("&").append("dateFrom=").append(dateFrom);
        sb.append("&").append("dateTo=").append(dateTo);
        
        return sb.toString();
    }
    
    private String formatTime(String isoTime) {
        Date date = DateUnit.getStandardOrISO(isoTime);

        String format = "yyyyMMdd";
        
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); //assumes GMT time zone
        String formattedTime = dateFormat.format(date);
        
        return formattedTime;
    }
    
}
