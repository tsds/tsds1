package lasp.tss.util;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;


public class RequestWrapper {

    String _remoteAddr;
	String _remoteHost;
    String _country;
    String _uri;
    String _pathInfo;
    String _remoteUser;
    String _query;
    String _servletPath;
    String _serverName;
    int    _serverPort;
    String _requestURI;
    String _forwarded_for;
    
    public RequestWrapper(HttpServletRequest request) {
	    _remoteAddr = request.getRemoteAddr();
	    _remoteHost = request.getRemoteHost();
	    Locale locale = request.getLocale();
	    _country = locale.getDisplayCountry();
	    _uri = request.getRequestURI();
	    _pathInfo = request.getPathInfo();
	    _remoteUser = request.getRemoteUser();
	    _query = request.getQueryString();
	    _servletPath = request.getServletPath();  
	    _serverName = request.getServerName();
	    _serverPort = request.getServerPort();
	    _requestURI = request.getRequestURI();
	    _forwarded_for = request.getHeader("X-Forwarded_For");
    }
    
    public void setLogInfo() {
    	/*
    	 * Note: these must agree with values in logback.xml
    	 */
	    MDC.put("remoteAddr", get_remoteAddr());
	    MDC.put("remoteHost", get_remoteHost());
	    MDC.put("forwarded_for", get_forwarded_for());
	    MDC.put("remoteUser", get_remoteUser());
	    MDC.put("uri", getMyUri());
    }
    
    public void clearLogInfo() {
	    MDC.clear();
    }
    
    public String get_forwarded_for() {
    	return _forwarded_for;
    }
    
    public String getMyUri() {
//    	return "//" + _serverName + ":" + _serverPort + _requestURI + "?" + _query;    		
    	return _requestURI + "?" + _query;    		
    }
    
    public String get_remoteAddr() {
		return _remoteAddr;
	}

	public String get_remoteHost() {
		return _remoteHost;
	}

	public String get_country() {
		return _country;
	}

	public String get_uri() {
		return _uri;
	}

	public String get_pathInfo() {
		return _pathInfo;
	}

	public void set_pathInfo(String info) {
		_pathInfo = info;
	}

	public String get_remoteUser() {
		return _remoteUser;
	}

	public String get_query() {
		return _query;
	}

	public String get_servletPath() {
		return _servletPath;
	}
    
}
