package lasp.tss.util;

import javax.servlet.http.HttpServletRequest;

public class ServerUtils {

    /**
     * Get the base URL (up through TSS servlet mapping) from the request.
     */
    public static String getServerUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString(); //full url (without query) 
        String pinfo = request.getPathInfo(); // data set name + suffix
        
        //Remove the path info from the url.
        //The test accounts for hitting just the servlet with no ending "/"
        if (pinfo != null && url.endsWith(pinfo)) {
            int index = url.lastIndexOf(pinfo);
            url = url.substring(0,index);
        }
        
        return url;
    }
    
}
