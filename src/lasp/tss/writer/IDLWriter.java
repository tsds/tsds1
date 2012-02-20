package lasp.tss.writer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;

import lasp.tss.TSSException;
import lasp.tss.TimeSeriesDataset;

/**
 * Write IDL code to read requested data.
 * Only works for scalar time series, for now.
 */
public class IDLWriter extends HtmlWriter implements DatasetWriter {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(IDLWriter.class);
    
    public void write() {
        String link = getServerUrl() + "/resources/tss_reader__define.pro";
        println("<pre>");
        println("; Make sure the TSS Reader (<a href="+link+">tss_reader__define.pro</a>) is in your IDL_PATH,");
        println("; then run the following IDL commands." );
        	
        //get data set name
        String dsname = getDataset().getName();
        
        //build the IDL command to create the TSS Reader
        StringBuilder sb = new StringBuilder("tss = OBJ_NEW('tss_reader'");
        
        //get base url
        String url = getDataset().getRequestUrl();
        int index = url.indexOf(dsname);
        String burl = url.substring(0, index);
        sb.append(", baseurl='");
        sb.append(burl);
        sb.append("'");
        sb.append(")");
        
        //print the IDL command to create the TSS Reader
        println(sb.toString());
        
        //build the IDL command to read the data
        sb = new StringBuilder("data = tss->read_data(dataset='");
        sb.append(dsname);
        sb.append("'");

        //get constraint expression
        String ce = getDataset().getConstraintExpression(); 
        if (ce != null) {
            try {
                //decode the URL so it looks more presentable
                ce = URLDecoder.decode(ce, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                String msg = "Unable to decode constraint expression: " + ce;
                _logger.error(msg, e);
                throw new TSSException(msg, e);
            }
            sb.append(", ce='");
            sb.append(ce);
            sb.append("'");
        }
        
        sb.append(")");
       
        //print the IDL command to read the data
        println(sb.toString());

        //print the IDL command to destroy the reader object
        println("OBJ_DESTROY, tss");

        println("</pre>");
    }

    
  //----- DatasetWriter Methods ----------------------------------------------
    private TimeSeriesDataset _dataset;
    public void setDataset(TimeSeriesDataset dataset) { _dataset = dataset; }
    public TimeSeriesDataset getDataset() { return _dataset; }
        
}
