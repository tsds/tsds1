<%@page import="java.io.*" %>
<%@page import="java.net.*" %>
<%@page import="java.util.*" %>
<%@page import="java.text.*" %>
<%@page import="java.util.regex.*" %>

<%@page import="javax.script.ScriptEngineManager" %>
<%@page import="javax.script.ScriptEngine" %>
<%@page import="javax.script.ScriptException" %>

<%@page import="org.json.simple.JSONArray" %>
<%@page import="org.json.simple.JSONObject" %>
<%@page import="org.json.simple.JSONValue" %>

<%@page import="org.apache.commons.codec.digest.*" %>
<%@page import="org.apache.commons.io.*" %>

<%@page trimDirectiveWhitespaces="true" %>

<%

	// TODO: Check that 
	// request.getSession().getServletContext().getRealPath("/") + "../TSDS/datasets/archive/
	// request.getSession().getServletContext().getRealPath("/") + "../TSDS/datasets/queries/
	// exists and are writeable.
	
	String list        = request.getParameter("list");
	String catalog     = request.getParameter("catalog");
	System.out.println(catalog);
	if (catalog == null || catalog.isEmpty()) {
		OutputStream r = response.getOutputStream();
		DataOutputStream os = new DataOutputStream(r);

		FileInputStream fin = new FileInputStream(getServletContext().getRealPath("/") + "index.htm");
		int totalBytes = IOUtils.copy(fin, os);
		//response.sendRedirect("tsdsfe/index.htm");
		request.getRequestURL().toString();
		return;
	}

	String dataset     = request.getParameter("dataset");
	String parameters  = request.getParameter("parameters");
	String version     = request.getParameter("version");
	String start       = request.getParameter("start");
	String stop        = request.getParameter("stop");
	String output      = request.getParameter("out");
	String attach      = request.getParameter("attach");
	
	String constraint  = request.getParameter("constraint");
	String filter      = request.getParameter("filter");
	String requestid   = request.getParameter("requestid");
	String updateData  = request.getParameter("updateData");
	String updateNcML  = request.getParameter("updateNcML");

	String all         = request.getParameter("all");
	String catalogs    = request.getParameter("catalogs");

	String stream      = request.getParameter("stream");	
	String debug       = request.getParameter("debug");

	String resp       = request.getParameter("return");

	if (debug == null || debug.isEmpty()) {
		debug = "false";
	}

	if (debug.equals("true")) {
		out.println("<html><body><pre>");
	}

	if (attach == null || attach.isEmpty()) {
		attach = "false";
	}
	
	// Read TSDSFE properties
	Properties prop = new Properties();
	String propfname = getServletContext().getRealPath("/") + "/tsdsfe.properties";
	FileInputStream pin = new FileInputStream(propfname);
	prop.load(pin);
	pin.close();

	String xslttransform = prop.getProperty("xslttransform");

	// Try to connect to metadata server that performs xslt transforms.
    try {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(xslttransform).openConnection();
        con.setRequestMethod("HEAD");
		if (con.getResponseCode() != 200)
			if (debug.equals("true")) {
				response.sendError(500, "TSDSFE cannot connect to xslt transform server.");	
				return;
			} else {
	        	out.println("Cannot connect to " + xslttransform + ".  Will send 500 error.  Response from URL was = " + con.getResponseCode());
				return;
			}
      }
      catch (Exception e) {
         e.printStackTrace();
      }

	// Read DataCache server URL
	String dcserver = prop.getProperty("dcserver"); 		
	if (debug.equals("true")) {
		out.println("<b>dcurl: </b>" + dcserver);out.flush();
	}

	// See if DataCache server is working.
	// TODO: If server is not up, don't need to fail if stream=true and stream cache is available.
    try {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(dcserver).openConnection();
        con.setRequestMethod("HEAD");
		if (con.getResponseCode() != 200)
			if (debug.equals("true")) {
				response.sendError(500, "TSDSFE cannot connect to DataCache server");	
				return;
			} else {
	        	out.println("Cannot connect to " + dcserver + ".  Will send 500 error.  Response from URL was = " + con.getResponseCode());
				return;
			}
      }
      catch (Exception e) {
         e.printStackTrace();
      }

	// Read list of all THREDDS catalogs in XML file refered to by variable "all" in tsdsfe.properties.
	if (all == null || all.isEmpty()) {
		all = prop.getProperty("all");
	}
	if (debug.equals("true")) {
		out.println("<b>all: </b>" + all);
	}

	// If no catalog requested.
	if (catalog == null || catalog.isEmpty()) {
		OutputStream rx = response.getOutputStream();
		DataOutputStream osx = new DataOutputStream(rx);	
		response.setContentType("text/html");
	}

	String alluploadedurl = prop.getProperty("alluploadedurl");
	String alluploaded = prop.getProperty("alluploaded");
	String uploaddir = prop.getProperty("uploaddir");

	String catalogfname = "";
	String catalogxml = "";
	Boolean posted = false;
	String catalogpath = "";
	
	System.out.println("Catalog: " + catalog);

	// POSTed catalog
	// TODO: Re-write this.  
	if (catalog.toLowerCase().startsWith("<")) {
		posted = true;
		if (debug.equals("true")) {
			out.println("<b>POSTed catalog: </b>" + catalog);out.flush();
		}
		catalogxml = catalog;
		// TODO: Use XPATH library.
		catalogfname  = catalogxml.replaceAll("\r\n"," ").replaceAll("\n","").replace("docu.*","").replaceFirst("<(.*?)>.*","$1").replaceFirst(".*id=\"(.*?)\".*","$1.xml");
		System.out.println("Catalog filename: " + catalogfname);
		catalog = catalogfname.replace(".xml","");
		if (debug.equals("true")) {
			out.println("<b>Catalog ID: </b>" + catalogfname);out.flush();
		}		
	}
	System.out.println(posted);
	System.out.println(catalogxml);
	// URL reference to catalog.
	if (catalog.toLowerCase().startsWith("http://") || posted) {
		
		if (!posted) {

			if (debug.equals("true")) {
				out.println("<b>Requesting: </b>" + catalog);out.flush();
			}
			try {
				catalogxml = IOUtils.toString(new URL(catalog));
		    } catch (Exception e) {
		    	response.sendError(500, "Error when attempting to download catalog " + catalog);	
		    	System.out.println(e);
		    }
			if (debug.equals("true")) {
				out.println("<b>Response: </b>\n<textarea rows=10 style='width:100%'>" + catalogxml + "</textarea>");out.flush();
			}
			
			// TODO: If URL contains ?, save file as server/path/name.thredds, where name is an attribute in the catalog node.
			//       If no ID in the catalog node, save file as server/path/md5querystring.thredds and set ID=md5querystring.thredds in catalog.
					
			// Catalog file name and ID are derived from URL of catalog.
			//catalogfname = catalog.replace("http://","");
		}

		// Write uploaded catalog.
		if (uploaddir == null || uploaddir.isEmpty()) {
			// If catalogsdir is not specified in tsdsfe.properties.
			uploaddir = getServletContext().getRealPath("/") + "uploads/";
			String catalogdname = uploaddir + FilenameUtils.getPath(catalogfname);
			System.out.println("<b>Writing:</b> " + catalogfname + " to " + catalogdname + "");
			if (debug.equals("true")) {
				out.println("<b>Writing:</b> " + catalogfname + " to " + catalogdname + "");out.flush();
			}
			System.out.println("<b>Writing:</b> " + catalogfname + " to " + catalogdname + "");
			FileUtils.forceMkdir(new File(catalogdname));
			FileUtils.writeStringToFile(new File(uploaddir + "/" + catalogfname),catalogxml);
		} else {
			System.out.println("<b>Writing:</b> " + catalogfname + " to " + uploaddir + "");
			if (debug.equals("true")) {
				out.println("<b>Writing:</b> " + catalogfname + " to " + uploaddir);out.flush();
			}
			FileUtils.forceMkdir(new File(uploaddir));
			FileUtils.writeStringToFile(new File(uploaddir + "/" + catalogfname),catalogxml);

			// When using Tomcat in Eclipse and uploading to a workspace directory, this is needed
			// (otherwise a manual F5 on the uploads directory is needed to trigger copy into servlet container)
			uploaddir = getServletContext().getRealPath("/") + "uploads/";
			FileUtils.forceMkdir(new File(uploaddir));
			FileUtils.writeStringToFile(new File(uploaddir + "/" + catalogfname),catalogxml);

		}

		// Update or create all.thredds for uploads
		
		String alluploadedfname = "";

		if (alluploaded == null || alluploaded.isEmpty()) {
			// If alluploaded is not specified in tsdsfe.properties.
			alluploadedfname = uploaddir + "/" + "all.thredds";
		} else {
			alluploadedfname = uploaddir + "/" + alluploaded;
			
			
		}
		
		String alluploadedxml = "";
		File f = new File(alluploadedfname);
		boolean incatalog = false;
		if (f.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(alluploadedfname));
			String line;
			if (debug.equals("true")) {
				out.println("<b>Found existing all.thredds file in uploads directory:</b>\n<textarea rows=10 style='width:100%'>");out.flush();
			}
			
			while ((line = br.readLine()) != null) {
				alluploadedxml = alluploadedxml + line;
				if (line.matches(".*" + catalogfname + ".*")) {
					incatalog = true;
				}
				if (debug.equals("true")) {
					out.println(line);
				}
			}
			if (debug.equals("true")) {
				out.println("</textarea>");
			}
			br.close();
		} else {
			if (debug.equals("true")) {
				out.println("<b>File " + alluploadedfname + " not found.  Will create new one.</b>");out.flush();
			}
			String catatt = "name=\"Time Series Data Server Uploads THREDDS Catalog\" xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
			alluploadedxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><catalog " + catatt + "</catalog>";
		}
		String newxml = "<catalogRef ID=\"" + catalog + "\" xlink:title=\"" + catalog + "\" xlink:href=\"" + alluploadedurl + "/" + catalogfname + "\"/>";

		alluploadedxml = alluploadedxml.replace("</catalog>",newxml+"</catalog>");

		if (incatalog) {
			if (debug.equals("true")) {
				out.println("<b>Reference to catalog already exists.  Will not modify </b>" + alluploadedfname + "");out.flush();
			}
		}

		if (!incatalog) {
			if (alluploaded == null || alluploaded.isEmpty()) {
				// If alluploaded is not specified in tsdsfe.properties.
				String alluploadeddname = getServletContext().getRealPath("/") + "uploads/";
	
				if (debug.equals("true")) {
					out.println("<b>Writing: " + alluploadedfname + " to " + alluploadeddname + "</b>");out.flush();
				}
				FileUtils.writeStringToFile(new File(alluploadedfname),alluploadedxml);
			} else {
				if (debug.equals("true")) {
					out.println("<b>Writing:</b> " + alluploadedfname);out.flush();
				}
				FileUtils.writeStringToFile(new File(alluploadedfname),alluploadedxml);
				
				// When using Tomcat in Eclipse and uploading to a workspace directory, this is needed
				// (otherwise a manual F5 on the uploads directory is needed to trigger copy into servlet container)
				String alluploadeddname = getServletContext().getRealPath("/") + "uploads/";
				FileUtils.writeStringToFile(new File(alluploadedfname),alluploadedxml);
				
			}
		}
		
		
	}
	
	String cataloglist = "";
	String cataloglistuploaded = "";
	String catalogthredds = "";
	if (catalog.equals(".*") || catalog.toLowerCase().startsWith("http://")) {
		// List all catalogs in all

		URL url00 = new URL(xslttransform + "?xml="+all+"&xpath=//*[local-name()='catalogRef']/concat('{value:\"',@*[local-name()='ID'],'\",label:\"',@*[local-name()='ID']/../@*[local-name()='title'],'\"}',',')");		
		if (debug.equals("true")) {
			out.println("<b>Requesting: </b>" + url00);out.flush();
		}
		cataloglist = IOUtils.toString(url00);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + cataloglist);out.flush();
		}
		url00 = new URL(xslttransform + "?xml="+alluploadedurl+"/"+alluploaded+"&xpath=//*[local-name()='catalogRef']/concat('{value:\"',@*[local-name()='ID'],'\",label:\"',@*[local-name()='ID']/../@*[local-name()='title'],'\"}',',')");		
		if (debug.equals("true")) {
			out.println("<b>Requesting: </b>" + url00);out.flush();
		}
		cataloglistuploaded = IOUtils.toString(url00);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + cataloglistuploaded);out.flush();
		}
		
		if ((dataset == null) || dataset.isEmpty())  {
			out.println("["+cataloglist+cataloglistuploaded+"]");
			return;
		}
	} else {
		// Look for catalog names in all                                                     
		URL url00 = new URL(xslttransform + "?xml="+all+"&xpath=//*[local-name()='catalogRef'%20and%20@ID='"+catalog+"']/@*[local-name()='href']");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url00);out.flush();
		}
		System.out.println(catalog);

		catalogthredds = IOUtils.toString(url00);	

		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + catalogthredds);out.flush();
		}

		if (catalogthredds.equals("")) {
			url00 = new URL(xslttransform + "?xml="+alluploadedurl+"/"+alluploaded+"&xpath=//*[local-name()='catalogRef'%20and%20@ID='"+catalog+"']/@*[local-name()='href']");
			catalogthredds = IOUtils.toString(url00);		
			if (debug.equals("true")) {
				out.println("<b>Resuesting: </b>" + url00);out.flush();
			}
			if (debug.equals("true")) {
				out.println("<b>Response: </b> " + catalogthredds);out.flush();
			}
		}
		// TODO: Check if empty.
	}

	String cataloginfo = "";
	if (dataset == null || dataset.isEmpty()) {
		URL url01 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=['catalog']/*[local-name()='documentation']/concat('{link:\"',@*[local-name()='href'],'\",title:\"',@*[local-name()='title'],'\"},')");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url01);out.flush();
		}
		cataloginfo = IOUtils.toString(url01);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + cataloginfo);out.flush();
		}
		if (!cataloginfo.equals("{link:\"\",title:\"\"},")) {
			out.println("["+cataloginfo+"{link:\""+catalogthredds+"\",title:\"Catalog\"}]");
		} else {
			out.println("[{link:\""+catalogthredds+"\",title:\"Catalog\"}]");
		}
		return;
	}

	String datasetlist = "";
	if (dataset.equals(".*")) {
		
		// Get list of all datasets nodes in catalog with an ID attribute.  Return a comma-separated list of IDs.
		URL url01 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'][count(@*[local-name()='ID'])>0]/concat('{value:\"',@*[local-name()='ID'],'\",label:\"',@*[local-name()='label'],'\",name:\"',@*[local-name()='name'],'\"},')");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url01);out.flush();
		}
		datasetlist = IOUtils.toString(url01);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + datasetlist);out.flush();
		}

		// Get a list of all datasets, even if no ID attribute.
		// TODO: Assume ID=name when ID='' or no ID attribute and use this for value.
		if (datasetlist.replaceAll(",","").equals("")) {
			url01 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset']/concat('{value:\"',@*[local-name()='ID'],'\",label:\"',@*[local-name()='label'],'\",name:\"',@*[local-name()='name'],'\"},')");
			if (debug.equals("true")) {
				out.println("<b>Resuesting: </b>" + url01);out.flush();
			}
			datasetlist = IOUtils.toString(url01);
			if (debug.equals("true")) {
				out.println("<b>Response: </b> " + datasetlist);out.flush();
			}			
		}
		if ((parameters == null) || parameters.isEmpty())  {
			out.println("["+datasetlist+"]");
			return;
		}
	} else {
		// TODO: Verify that dataset exists.
	}

	
	String startavailable = "";
	String startdefault = "";
	if (!(start == null || start.isEmpty()) && start.equals(".*")) {
		URL url04 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='timeCoverage']//*[local-name()='Start']/text()");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url04);out.flush();
		}
		startavailable = IOUtils.toString(url04);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + startavailable);out.flush();
		}
		out.println(startavailable.substring(0,10));
		return;
	} else {
		URL url04 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='timeCoverage']//*[local-name()='Start']/text()");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url04);out.flush();
		}
		startdefault = IOUtils.toString(url04).substring(0,10);	
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + startdefault);out.flush();
		}
		if (start == null || start.isEmpty()) {
			start = startdefault;
			if (debug.equals("true")) {
				out.println("<b>No start date requested.  Using default start date </b>: " + start);out.flush();
			}
		}
	}

	String stopavailable = "";
	String stopdefault = "";
	if ((!(stop == null || stop.isEmpty()) && stop.equals(".*"))) {
		URL url05 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='timeCoverage']//*[local-name()='End']/text()");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url05);out.flush();
		}
		stopavailable = IOUtils.toString(url05);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + stopavailable);out.flush();
		}
		// Use start + 5 days for now.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(sdf.parse(start));
		c.add(Calendar.DATE, 2);
		stopavailable = sdf.format(c.getTime());
		out.println(stopavailable.substring(0,10));
		return;
	} else {
		URL url05 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='timeCoverage']//*[local-name()='End']/text()");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url05);out.flush();
		}
		stopdefault = IOUtils.toString(url05).substring(0,10);	
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + stopdefault);out.flush();
		}

		if (stop == null || stop.isEmpty()) {
			
			// Use start + 10 days for now.
			// TODO: Put this in properties file.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar c = Calendar.getInstance();
			c.setTime(sdf.parse(start));
			c.add(Calendar.DATE, 10);
			stopdefault = sdf.format(c.getTime());
			if (stop == null || stop.isEmpty()) {
				stop = stopdefault;
				if (debug.equals("true")) {
					out.println("<b>No stop date requested.  Using default stop date </b>: " + stop);out.flush();
				}
			}
		}
	}

	String datasetinfo = "";
	if (parameters == null || parameters.isEmpty()) {
		URL url01 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']/*[local-name()='documentation']/concat('{link:\"',@*[local-name()='href'],'\",title:\"',@*[local-name()='title'],'\"},')");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url01);out.flush();
		}
		datasetinfo = IOUtils.toString(url01);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + datasetinfo);out.flush();
		}
		out.println("["+datasetinfo+"]");
		return;
	}

	if (parameters == null || parameters.isEmpty()) {
		response.sendError(400, "A parameter must be specified, e.g., tsdsfe?catalog=sscweb&dataset=ace&parameters=X_GEI");
        return;
    }

	String parameterslist = "";
	String groupslist = "";
	if (parameters.equals(".*")) {
		URL url03 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='variable']/concat('{value:\"',@*[local-name()='id'],'\",label:\"',@*[local-name()='label'],'\",name:\"',@*[local-name()='name'],'\"},')");
		if (debug.equals("true")) {
			out.println("<b>-Resuesting: </b>" + url03);out.flush();
		}
		parameterslist = IOUtils.toString(url03);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + parameterslist);out.flush();
		}
		url03 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='group']/concat('{value:\"',@*[local-name()='elements'],'\",label:\"',@*[local-name()='label'],'\"},')");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url03);out.flush();
		}
		groupslist = IOUtils.toString(url03);
		if (debug.equals("true")) {
			out.println("<b>Response: </b> " + parameterslist);out.flush();
		}
		// TODO: Get columns by looking up variable ids in variable list.
		//System.out.println(groupslist);
		if (groupslist.matches("\\{value:\"\",label:\"\"\\},") && parameterslist.matches("\\{value:\"\",label:\"\"\\},")) {
			url03 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='access'%20and%20@serviceName='tss']/concat('{value:\"',@*[local-name()='urlPath'],'\",label:\"',@*[local-name()='urlPath'],'\"},')");
			if (debug.equals("true")) {
				out.println("<b>Resuesting: </b>" + url03);out.flush();
			}
			parameterslist = IOUtils.toString(url03);
			if (debug.equals("true")) {
				out.println("<b>Response: </b> " + parameterslist);out.flush();
			}	
			out.println("["+parameterslist+"]");
		} else {
			out.println("["+groupslist+parameterslist+"]");
		}
		return;
	} else {
		// TODO: Verify that parameter exists.
	}	

	if (version == null || version.isEmpty()) {
		version = "0";
	}
	
	if (debug == null || debug.isEmpty()) {
		debug = "false";
	}
	
	if (updateData == null || updateData.isEmpty()) {
		updateData = "true";
	}

	if (updateNcML == null || updateNcML.isEmpty()) {
		updateNcML = "true";
	}

	if (output == null || output.isEmpty()) {
		output = "asc";
	}

	if (resp == null || resp.isEmpty()) {
		resp = "";
	}

	boolean archive = false;
	if (requestid != null && !requestid.isEmpty() && !requestid.equals("null")) {
		archive = true;
	}
		
	if (archive == true) {
		// TODO: If number of request parameters = 1, see if request ID exists by searching through all NcML files in archive.
	}

	String dcurl = prop.getProperty("dcurl"); 		
	if (debug.equals("true")) {
		out.println("<b>dcurl: </b>" + dcurl);out.flush();
	}

	String tsdsfe = prop.getProperty("tsdsfe"); 
	if (tsdsfe == null || tsdsfe.isEmpty()) {
		tsdsfe = "/tsdsfe";
	}
	if (debug.equals("true")) {
		out.println("<b>tsdsfe: </b>" + tsdsfe);out.flush();
	}

	String tsds = prop.getProperty("tsds"); 
	if (tsds == null || tsds.isEmpty()) {
		tsds = "/";
	}
	if (debug.equals("true")) {
		out.println("<b>tsds: </b>" + tsds);out.flush();
	}

	String dcdir = prop.getProperty("dcdir"); 
	if (dcdir == null || dcdir.isEmpty()) {
		dcdir = "/tmp";
	}
	if (debug.equals("true")) {
		out.println("<b>dcdir: </b>" + dcdir);out.flush();
	}

	// Always stream if browser to prevent user from saving link to a redirect URL that may change.
	// If no stream parameter in request, look to see if client's user agent string
	// is found in clients.xml.  If it is found, use the specified stream option.
	if (stream == null || stream.isEmpty()) {
		String clients = prop.getProperty("clients"); 
		if (debug.equals("true")) {
			out.println("<b>clients: </b>" + clients);out.flush();
		}
		String useragent = request.getHeader("user-agent");

		// Look for useragent in clients.xml
		URL url04 = new URL(xslttransform + "?xml="+clients+"&xpath=//client/regex");
		if (debug.equals("true")) {
			out.println("<b>Resuesting: </b>" + url04);out.flush();
		}
		String regexs = IOUtils.toString(url04);
		String[] Regexs = regexs.replace("<regex>","").replace("</regex>","").split("\n");
	
		for (int j = 0;j < Regexs.length;j++) {
			if (useragent.matches(Regexs[j])) {
				if (debug.equals("true")) {
					out.println("<b>Match: </b> " + Regexs[j] + " to " + useragent);out.flush();
				}
				// Look for useragent in clients.xml
				URL url05 = new URL(xslttransform + "?xml="+clients+"&xpath=//client/regex");
				if (debug.equals("true")) {
					out.println("<b>Resuesting: </b>" + url05);out.flush();
				}
				stream = IOUtils.toString(url05);
			} else {
				if (debug.equals("true")) {
					out.println("<b>No match: </b> " + Regexs[j] + " to " + useragent);out.flush();
				}				
			}
		}
		
		if (stream == null || stream.isEmpty()) {				
			stream = "true";
		} else {
			stream = "false";
		}
		if (debug.equals("true")) {
			out.println("<b>Stream: </b> " + stream);out.flush();
		}				

	}

	String servers = prop.getProperty("servers"); 
	if (servers == null || servers.isEmpty()) {
		servers = "http://tsds.net/tsdsdev";
	}
	if (debug.equals("true")) {
		out.println("<b>servers: </b>" + servers);out.flush();
	}

	// Look for dcsubdir in servers.xml
	URL url03 = new URL(xslttransform + "?xml="+servers+"&xpath=//server/id[text()='"+catalog+"']/../dir/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url03);out.flush();
	}
	String dcsubdir = IOUtils.toString(url03);	
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + dcsubdir);out.flush();
	}
	if (debug.equals("true")) {
		out.println("<b>dcsubdir: </b> " + dcsubdir);out.flush();
	}

	// Location to cached TSDS responses.
	String tsdsfedcDir = request.getRemoteAddr();
	if (debug.equals("true")) {
		out.println("<b>tsdsfedcDir: </b> " + tsdsfedcDir);out.flush();
	}
	if (!(new File(tsdsfedcDir)).exists()) {
		// TODO: Create directory?
	}
	
	// Read catalogs.xml and extract urlgenerator for requested catalog.
	if (catalogs == null || catalogs.isEmpty()) {
		catalogs = prop.getProperty("catalogs"); 
	}
	if (debug.equals("true")) {
		out.println("<b>catalogs: </b>" + catalogs);out.flush();
	}
	URL url04 = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../urlgenerator/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url04);out.flush();
	}
	String urlgenerator = IOUtils.toString(url04);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + urlgenerator);out.flush();
	}
	if (debug.equals("true")) {
		out.println("<b>urlgenerator: </b>" + urlgenerator);out.flush();
	}

	// Read catalogs.xml and extract urlpreprocess for requested catalog.
	if (catalogs == null || catalogs.isEmpty()) {
		catalogs = prop.getProperty("catalogs"); 
	}
	if (debug.equals("true")) {
		out.println("<b>catalogs: </b>" + catalogs);out.flush();
	}

	url04 = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../urlpreprocess/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url04);out.flush();
	}
	String urlpreprocess = IOUtils.toString(url04);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + urlgenerator);out.flush();
	}
	if (debug.equals("true")) {
		out.println("<b>urlgenerator: </b>" + urlgenerator);out.flush();
	}

 	// Read catalogs.xml and extract ncmlgenerator for requested catalog.
	URL url05 = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../ncml/generator/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05);out.flush();
	}
	String ncmlgenerator = IOUtils.toString(url05);
	if (ncmlgenerator.length() == 0) {
		ncmlgenerator = prop.getProperty("ncmlgenerator");
	}
	// TODO: Error if ncmlgenerator.length() == 0.

	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + ncmlgenerator);out.flush();
	}
	if (debug.equals("true")) {
		out.println("<b>ncmlgenerator: </b>" + ncmlgenerator);out.flush();
	}
	
 	// Read catalogs.xml and extract IOSP for requested catalog.
 	// TODO: Only do this if IOSP is not specified in catalog.
	URL url05b = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../ncml/iosp/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05b);out.flush();
	}
	String iosp = IOUtils.toString(url05b);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + iosp);out.flush();
	}
	if (debug.equals("true")) {
		out.println("<b>IOSP: </b>" + iosp);out.flush();
	}

 	// Read catalogs.xml and extract timecols for requested catalog.
 	// TODO: Only do this if IOSP is not specified in catalog.
	URL url05c = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../ncml/timecols/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05c);out.flush();
	}
	String timecols = IOUtils.toString(url05c);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + timecols);out.flush();
	}

 	// Read catalogs.xml and extract timefmt given catalog.
 	// TODO: Only do this if timefmt is not specified in catalog.
	URL url05d = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../ncml/timefmt/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05d);out.flush();
	}
	String timefmt = IOUtils.toString(url05d);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + timefmt);out.flush();
	}

 	// Read catalogs.xml and extract timefmt given catalog.
 	// TODO: Only do this if timefmt is not specified in catalog.
	url05d = new URL(xslttransform + "?xml="+catalogs+"&xpath=//catalog/id[text()='"+catalog+"']/../ncml/fillvalue/text()");
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05d);out.flush();
	}
	String fillvalue = IOUtils.toString(url05d);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + fillvalue);out.flush();
	}

 	// Read THREDDS catalogs and extract information that may not be available in catalogs.xml.
 	String concatstr = "concat(@*[local-name()='id'],'_,_',@*[local-name()='name'],'_,_',@*[local-name()='label'],'_,_',@*[local-name()='units'],'_,_',@*[local-name()='type'],'_,_',@*[local-name()='format'],'_,_',@*[local-name()='fillvalue'],'_,_',@*[local-name()='urlgenerator'],'_,_',@*[local-name()='urltemplate'],'_,_',@*[local-name()='urlpreprocess'],'_,_',@*[local-name()='timecolumns'],'_,_',@*[local-name()='timeformat'],'_,_',@*[local-name()='timestart'],'_,_',@*[local-name()='timeend'],'_,_',@*[local-name()='columns'],'_,_',@*[local-name()='lineregex'])";
	String[] varprops = {};
	
	// TODO: Create string with all keys and use to populate hashmap and url05.

	url05 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']/"+concatstr);

 	//url05 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='processing']//*[local-name()='variable']/"+concatstr);
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05);out.flush();
	}
	String varinfo = IOUtils.toString(url05);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + varinfo);out.flush();
	}

	varprops = varinfo.split("_,_",-1);

	String names = varprops[1];
	String units = varprops[3];
	
	if (fillvalue.length() == 0) {
		fillvalue = varprops[6];
	}

	if (urlgenerator.length() == 0) {
		urlgenerator = varprops[7];
	}

	String urltemplate = varprops[8];

	if (urlpreprocess.length() == 0) {
		urlpreprocess = varprops[9];
	}

	String timecolumns = varprops[10];
	String timeformat = varprops[11];

	String columns = varprops[14];
	String lineregex = varprops[15];
	
	iosp = "lasp.tss.iosp.ColumnarAsciiReader";
	System.out.println(urltemplate);

	if (dcsubdir.length() == 0) {
		dcsubdir = urltemplate.split("/")[2];
		if (debug.equals("true")) {
			out.println("<b>dcsubdir is empty.  Deriving from urltemplate: </b>" + dcsubdir);out.flush();
		}		
	}

	url05 = new URL(xslttransform + "?xml="+catalogthredds+"&xpath=//*[local-name()='dataset'%20and%20@ID='"+dataset+"']//*[local-name()='variable'%20and%20@id='"+URLEncoder.encode(parameters)+"']/"+concatstr);
	if (debug.equals("true")) {
		out.println("<b>Resuesting: </b>" + url05);out.flush();
	}
	varinfo = IOUtils.toString(url05);
	if (debug.equals("true")) {
		out.println("<b>Response: </b> " + varinfo);out.flush();
	}
	varprops = varinfo.split("_,_",-1);

	columns = varprops[14];

	if (varprops[1].length() != 0) {
		names = varprops[1];
	}
	if (varprops[3].length() != 0) {
		units = varprops[3];
	}
	String format = "";
	if (varprops[5].length() != 0) {
		format = varprops[5];
	}
	if (varprops[6].length() != 0) {
		fillvalue = varprops[6];
	}
	if (varprops[7].length() != 0) {
		urlgenerator = varprops[7];
	}
	if (varprops[8].length() != 0) {
		urltemplate = varprops[8];
	}
	if (varprops[9].length() != 0) {
		urlpreprocess = varprops[9];
	}
	if (varprops[10].length() != 0) {
		timecolumns = varprops[9];
	}
	if (varprops[11].length() != 0) {
		timeformat = varprops[10];
	}
	
//	if (varprops[12].length() != 0) {
//		timestart = varprops[12];
//	}
//	if (varprops[13].length() != 0) {
//		timeend = varprops[13];
//	}
	if (varprops[13].length() != 0) {
		timeformat = varprops[11];
	}
	if (varprops[15].length() != 0) {
		lineregex = varprops[15];
	}

	if (debug.equals("true")) {
		out.println("<b>timecolumns: </b> " + timecols);out.flush();
	}

	// If request was for MATLAB or IDL download code, modify the script and return it.
	// TODO: If large request, modify code to make request in chunks.
	if (resp.equals("matlab") || resp.equals("idl")) {
		String script = "";
		if (resp.equals("matlab")){
			script = getServletContext().getRealPath("/") + "scripts/bootstrap.m";
		}
		if (resp.equals("idl")){
			script = getServletContext().getRealPath("/") + "scripts/bootstrap.pro";
		}
		java.util.Scanner ss = new java.util.Scanner(new File(script)).useDelimiter("\\A");
		String code = ss.hasNext() ? ss.next() : "";

		code = code.replace("__QUERYSTRING__","catalog="+catalog+"&dataset="+dataset+"&parameters="+parameters+"&version="+version+"&start="+start+"&stop="+stop);
		code = code.replace("__SERVER__",tsdsfe+"/");
		if (debug.equals("true")) {
			out.println("<b>Response will be this code: </b>\n" + code);
		} else {
			response.setContentType("text/plain");
			out.println(code);
			return;
		}
	}

	// Form request ID based on first part of request URL
	String requestid0    = "catalog="+catalog+"&dataset="+dataset+"&parameters="+parameters+"&version="+version+"&start="+start+"&stop="+stop+"&requestid="+requestid;
	String requestid0md5 = DigestUtils.md5Hex(requestid0);

	// Form secondary request id based on last part of request URL.
	String requestid1    = requestid0 + "&constraint=" + constraint + "&filter=" + filter;
	String requestid1md5 = DigestUtils.md5Hex(requestid1);
	
	if (debug.equals("true")) {
		out.println("<b>Resuest ID1 string: </b>" + requestid0);out.flush();
		out.println("<b>Resuest ID1 md5: </b>"    + requestid0md5);out.flush();
		out.println("<b>Resuest ID2 string: </b>" + requestid1);out.flush();
		out.println("<b>Resuest ID2 md5: </b>"    + requestid1md5);out.flush();
	}
	
	// Query ID is MD5 of catalog+dataset+parameters+version+start+stop+requestid
	String qid = requestid0md5;

	// Location to place the archive NcML file (if needed).
	//String fnamearchive = getServletContext().getRealPath("/") + "/archive/" + qid + ".ncml";
	String fnamearchive = request.getSession().getServletContext().getRealPath("/") + "../TSDS/datasets/archive/" + qid + ".ncml";
	File fa = new File(fnamearchive);
	
	// Location to place the query NcML file.
	//String fname = getServletContext().getRealPath("/") + "/queries/" + qid + ".ncml";
	String fname = request.getSession().getServletContext().getRealPath("/") + "../TSDS/datasets/queries/" + qid + ".ncml";
	File f = new File(fname);

	System.out.println("----------------");
	System.out.println(fname);
	String fnamesave = "";
	if (archive == true) {
		fnamesave = fnamearchive;
	} else {
		fnamesave = fname;
	}

	// Determine if request for list of files associated with the request can be skipped.
	if (fa.exists() && updateNcML.equals("false")) {
		if (debug.equals("true")) {
			out.println("<b>Found existing query archive NcML file archive directory.  Skipping call to DataCache. </b>" + fnamearchive);out.flush();
		}
		if (updateNcML.equals("true")) {
			if (archive == true && updateNcML.equals("true")) {
				if (debug.equals("true")) {
					out.println("<b><font color='red'>Error.  Data was previously requested with same options and a requestid; update=true is not allowed.  Either use a different requestid or set update=false. Responding with 400.</font></b>");
					return;
				} else {
					response.sendError(400, "Data was previously requested with same options and a requestid; update=true is not allowed.  Either use a different requestid or set update=false.");
					return;
				}
			}
		}
	} else {
		if (debug.equals("true")) {
			out.println("<b>Did not find existing query archive NcML file in archive directory. </b>" + fnamearchive);out.flush();					
		}
		if (f.exists()) {
			if (debug.equals("true")) {
				out.println("<b>Found existing query NcML file. </b>" + fname);out.flush();
				// TODO: Check that local files referenced in NcML file exist and are not tainted.
				if (updateNcML.equals("true")) {
					out.println("<b>Will overwrite existing query NcML file because update=true. </b>");out.flush();
				}
			}
		} else {
			if (debug.equals("true")) {
				out.println("<b>Did not find existing query NcML file. </b>" + fname);out.flush();			
			}
		}
	}

	// Take action if new query NcML is needed.
	if (updateNcML.equals("true") || (!fa.exists() && !f.exists())) {

		// TODO:
		// Try all servers for datasets/queries/qid.ncml or datasets/archive/queries/qid.ncml listed in dccaches.xml and dcarchives.xml.
		
		if (debug.equals("true")) {
				out.println("<b>Creating NcML file. </b>" + fname);out.flush();
		}

		String urilist = "";
		String poststr = "";
	    if (urltemplate.length() == 0) {
			// Generate list of URLs to send to DataCache
			String listuri = xslttransform + "?xslt="+urlgenerator+"&dataset="+dataset+"&parameters="+URLEncoder.encode(parameters)+"&start="+start+"&stop="+stop+"&urltemplate="+URLEncoder.encode(urltemplate.replace("&amp;","&"))+"&urlpreprocess="+URLEncoder.encode(urlpreprocess);
			
			if (archive == true && !fa.exists()) {
				if (debug.equals("true")) {
					out.println("<b>Requestid specified.  Archiving granules specified in NcML file if necessary. </b>");
				}
				listuri = listuri + "&requestid=" + requestid;
			}
	
			if (debug.equals("true")) {
				out.println("<b>Requesting: </b>" + listuri);out.flush();
			}
	
			URL url0 = new URL(listuri);
			urilist = IOUtils.toString(url0);

			// Return list of URIs
			if (resp.equals("urilist")) {
				response.setContentType("text/plain");
				out.println(urilist);
				System.out.println("Returning URI list");
				return;
			}

			if (debug.equals("true")) {
				out.println("<b>Response: </b>\n<textarea rows=10 style='width:100%'>" + urilist + "</textarea>");out.flush();
			}
	
			// Determine maximum number of simultaneous requests to make to data server.  	
			URL url06 = new URL(xslttransform + "?xml="+servers+"&xpath=//server/id[text()='"+catalog+"']/../max/text()");
			if (debug.equals("true")) {
				out.println("<b>Resuesting: </b>" + url06);out.flush();
			}
			String max = IOUtils.toString(url06);	
			if (debug.equals("true")) {
				out.println("<b>Response: </b> " + max);out.flush();
			}
			int nurls = urilist.split("\n").length;
			if ( nurls >  Integer.parseInt(max)) {
				if (debug.equals("true")) {
					out.println("<b>Number of URLs in list ("+nurls+") exceeds max specified in servers.xml ("+max+").  Splitting list.</b> ");out.flush();
				}			
				// TODO: Split list into chunks based on max.
			}
			poststr = "includeMeta=true&source="+URLEncoder.encode(URLDecoder.decode(urilist,"UTF-8"),"UTF-8");

	    } else {
	    	poststr = "includeMeta=true&timeRange="+start+"/"+stop+"&template="+URLEncoder.encode(urltemplate.replace("&amp;","&"));
	    }

		if (updateData.equals("true")) {
			poststr = "forceWrite=true&forceUpdate=true&" + poststr;
		}
		
		if (lineregex.length() > 0) {
			poststr = "lineRegExp="+URLEncoder.encode(lineregex,"UTF-8")+"&"+poststr;
		}
			
		// Tell DataCache to store granules in a special archive directory.
		if (archive == true && !fa.exists()) {
			poststr = "dir=" + tsdsfedcDir + "/archive/" + dcsubdir + "&" + poststr;
			if (debug.equals("true")) {		
				out.println("<b>Requestid specified.  Archiving granules specified in NcML file. </b>");out.flush();
			}
		}


		if (debug.equals("true")) {
			out.println("<b>Posting to " + dcserver + ": </b>" + poststr);out.flush();
		}

		// Post list of URLs to DataCache server.
		URL url = new URL(dcserver);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(poststr);
		wr.flush();

		
		// Read response from DataCache.
		InputStreamReader is = new InputStreamReader(conn.getInputStream());
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");

		String json = s.hasNext() ? s.next() : "";
		if (debug.equals("true")) {
			out.println("<b>Response: </b>\n<textarea rows=10 style='width:100%'>" + json + "</textarea>");out.flush();
		}
		
		// http://code.google.com/p/json-simple/
		// docs: http://alex-public-doc.s3.amazonaws.com/json_simple-1.1/index.html
		// Parse JSON response from DataCache.
		// TODO: Modify DataCache to have XML version of JSON response.
		JSONArray ja = (JSONArray)JSONValue.parse(json);
		JSONObject jo = (JSONObject)ja.get(0);
		if (debug.equals("true")) {
			out.println("<b>First data url</b>: <a href='"+dcurl+"/"+dcsubdir+"/"+jo.get("urlMd5")+".data'>"+jo.get("urlMd5")+".data"+"</a>");out.flush();
			out.println("<b>First metadata url</b>: <a href='"+dcurl+"/"+dcsubdir+"/"+jo.get("urlMd5")+".datax'>"+jo.get("urlMd5")+".datax"+"</a>");out.flush();
			if (jo.containsKey("metaJson")) {
				out.println("<b>First metaJson</b>: " + jo.get("metaJson"));out.flush();
			}	
		}

		if (resp.equals("report")) {
			response.setContentType("text/plain");
			out.println(json);
			System.out.println("Returning DataCache report");
			return;
		}

		// Create an XML list of files from JSON to post to xml2ncml.
		String filelist = "<files>";
		for (int i = 0; i < ja.size(); i++) {
			filelist = filelist + "<file>" + ((JSONObject)ja.get(i)).get("urlMd5") + "</file>"; 
		}
		filelist = filelist + "</files>";
		System.out.println(filelist);
		
		String variablenames = "";
		String variablecols = "";
		String groupname = parameters;
		String variablelong_names = "";
		String variableunits = "";
		
		if (!jo.containsKey("metaJson") || jo.get("metaJson").toString().equals("{}")) {
			if (debug.equals("true")) {
				out.println("<b>No column label metadata returned by DataCache.</b>");out.flush();
			}
		} else {			
			JSONArray metaJson = (JSONArray)jo.get("metaJson");
			JSONArray metaColumnLabels = (JSONArray)metaJson.get(0);
			JSONArray metaColumnUnits = (JSONArray)metaJson.get(1);
			if (debug.equals("true")) {
				out.println("<b>Number of columns</b>: " + metaColumnLabels.size());
				out.println("<b>Column labels</b>: " + metaColumnLabels.toString());
			}
			
			String[] metaColumnLabelsA = new String[metaColumnLabels.size()];
			String[] metaColumnUnitsA = new String[metaColumnUnits.size()];

			for (int z = 0; z < metaColumnLabels.size();z++) {
				metaColumnLabelsA[z] = (String) metaColumnLabels.get(z);
			}
			for (int z = 0; z < metaColumnUnits.size();z++) {
				metaColumnUnitsA[z] = (String) metaColumnUnits.get(z);
			}

			String[] timecolsS = timecols.split(",");			
			int[] timecolsA = new int[timecolsS.length];
			
			for (int i = 0; i < timecolsS.length; i++) {
			    try {
			        timecolsA[i] = Integer.parseInt(timecolsS[i]);
			    } catch (NumberFormatException nfe) {
			    	System.out.println("NumberFormatException");
			    };
			}
			System.out.println(metaColumnLabels.size() == (timecolsS.length + 1));
			System.out.println(timecolsS.length);
			System.out.println(metaColumnLabelsA[0]);
			System.out.println(metaColumnUnitsA[2]);
			if (metaColumnLabels.size() == (timecolsS.length + 1)) {
				variablenames = metaColumnLabelsA[timecolsS.length];
				variablelong_names = variablenames;
				variablenames = parameters;
				variableunits = metaColumnUnitsA[timecolsS.length];
				variablecols = Integer.toString(timecolsS.length+1);
				System.out.println(variablecols);

				if (debug.equals("true")) {
					out.println("<b>Scalar time series.  Groupname = parametername = </b> " + variablenames);
					out.println("<b>Scalar time series.  long_name = column label in file = </b> " + variablelong_names);
					out.println("<b>Scalar time series.  name      = parametername = </b> " + variablenames);
				}
			} else {
				variablenames =  metaColumnLabelsA[timecolsS.length];
				variableunits =  metaColumnUnitsA[timecolsS.length];
				variablecols = Integer.toString(timecolsS.length+1);
				for (int i=timecolsS.length+1; i < metaColumnLabelsA.length; i++) {
					variablenames = variablenames + "," + metaColumnLabelsA[i];
					variableunits = variableunits + "," + metaColumnUnitsA[i];
					variablecols = variablecols + ";" + Integer.toString(i+1);
				}
				variablelong_names = variablenames;
	
				if (debug.equals("true")) {
					out.println("<b>Non-scalar time series.  Group label = </b>" + parameters);
					out.println("<b>Non-scalar time series.  Parameter labels and columns and units = </b>" + variablenames + " " + variablecols + " " + variableunits);
				}
			}
		}
		
		int N = ja.size();
		String emsg = "";
		for (int i = 0;i < N;i++){
			jo = (JSONObject)ja.get(i);
			emsg = jo.get("error").toString();
			if (!emsg.equals("false")) {
				if (archive == true && updateData.equals("true")) {
					if (debug.equals("true")) {
						out.println("<b>Error. One of the granules returned an error.  Responding with 400.</b>");out.flush();
					} else {
						response.sendError(400, "One of the granules returned an error.");
						return;
					}
				}
			}
		}

		if (timecols.length() == 0) {
			timecols = timecolumns;
		}
		if (timefmt.length() == 0) {
			timefmt = timeformat;
		}

		if (variableunits.length() == 0) {
			variableunits = units;
		}
		if (variablelong_names.length() == 0) {
			variablelong_names = names;
		}
		if (variablenames.length() == 0) {
			variablenames = parameters;
		}
		if (variablecols.length() == 0) {
			variablecols = columns;
		}
		if (debug.equals("true")) {
			out.println("<b>columns: </b>" + columns);out.flush();
		}

		if (debug.equals("true")) {
			out.println("<b>Units: </b>" + variableunits);out.flush();
		}

		// Post list of files along with other needed NcML information to NcML generator XSLT.
		String xml2ncml = xslttransform + "?xslt="+ncmlgenerator+"&qid="+qid+"&requestid="+requestid+"&path=file://"+dcdir+"/"+dcsubdir+"&query="+tsdsfe+"?"+URLEncoder.encode(requestid0,"UTF-8");
		xml2ncml = xml2ncml + "&iosp="+iosp+"&timecols="+timecols+"&timefmt="+URLEncoder.encode(timefmt,"UTF-8")+"&groupname="+URLEncoder.encode(groupname,"UTF-8")+"&variablenames="+URLEncoder.encode(variablenames,"UTF-8")+"&variablelong_names="+URLEncoder.encode(variablelong_names,"UTF-8")+"&variablecols="+variablecols+"&units="+URLEncoder.encode(variableunits,"UTF-8")+"&fillvalue="+URLEncoder.encode(fillvalue,"UTF-8")+"&format="+URLEncoder.encode(format,"UTF-8");
		System.out.println("HERE"+URLEncoder.encode(fillvalue+"","UTF-8"));
		if (debug.equals("true")) {
			out.println("<b>Posting XML list of files from DataCache to: </b>" + xml2ncml);out.flush();
		}
		
		// TODO: Use IOUtils here.
		URL url2 = new URL(xml2ncml);
		HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
		conn2.setDoOutput(true);
		conn2.setRequestProperty("Content-Type","text/xml");
		OutputStreamWriter wr2 = new OutputStreamWriter(conn2.getOutputStream());
		wr2.write(filelist);
		wr2.flush();
		
		InputStreamReader is2 = new InputStreamReader(conn2.getInputStream());
		
		String mess2=conn2.getResponseMessage();
		if (debug.equals("true")) {	
			out.println("<b>Response message: </b>"+mess2);
		}
		
		java.util.Scanner s2 = new java.util.Scanner(is2).useDelimiter("\\A");
		String xml = s2.hasNext() ? s2.next() : "";
		if (debug.equals("true")) {
			out.println("<b>Response: </b>\n<textarea rows=10 style='width:100%'>" + xml + "</textarea>");out.flush();
			if (archive == true) {
				out.println("<b>Writing: </b>" + fnamesave);out.flush();
			} else {
				out.println("<b>Writing: </b>" + fnamesave);out.flush();
			}
		}
		
		// Save the NcML file.
		FileOutputStream fos = new FileOutputStream(fnamesave);
		PrintWriter pw = new PrintWriter(fos);
		pw.print(xml);
		pw.close();
		fos.close();

		if (debug.equals("true")) {
			out.println("<b>Writing complete.</b>");out.flush();
		}
	}
	
//	if (false) {
	String durla = "";
	String durl = "";
	if (archive == true) {
		durl = tsds + "/archive/" + qid + "."+output+"?time," + parameters;
		durl = durl + "&time>="+start+"&time<="+stop;
	} else {
		durl = tsds +"/queries/" + qid + "."+output+"?time," + parameters;
		durl = durl + "&time>="+start+"&time<="+stop;
	}
	if (constraint != null && !constraint.isEmpty()) {
		durl = durl + "&" + constraint; 
	}
	if (filter != null && !filter.isEmpty()) {
		durl = durl + "&" + filter; 
	}
	System.out.println(filter);

	String poststr2 = URLEncoder.encode(URLDecoder.decode(durl,"UTF-8"),"UTF-8");
	String amd5 = DigestUtils.md5Hex(durl);
	if (debug.equals("true")) {
		out.println("<b>Computed syncsubmit md5 filename: </b>" + amd5 + " = md5("+durl+")");out.flush();
	}

	String fname3 = "";
	if (archive == true) {
		fname3 = dcdir + "/" + tsdsfedcDir + "/archive/" + dcsubdir + "/" + amd5 + ".out";
	} else {
		fname3 = dcdir + "/" + tsdsfedcDir + "/queries/" + dcsubdir + "/" + amd5 + ".out";
	}
	File f3 = new File(fname3);
	
	boolean f3tainted = false;
	// When testing, sometimes bad NcML is created, in which case TSDS returns an HTML error message.
	// Check for this condition in cached granules.
	if (f3.exists()) {
		if (debug.equals("true")) {
			out.println("<b>Checking first 10 lines of cached stream file to see if it was an error response.</b>");out.flush();
		} 
		BufferedReader br = new BufferedReader(new FileReader(fname3));
		String line;
		Integer N = 0;
		
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		   if (N == 10) {
			   break;
		   }
		   N = N+1;
		   if (line.matches(".*Time Series Server Error.*")) {
				if (debug.equals("true")) {
					out.println("<b>Cached stream is tainted. </b> " + fname3);out.flush();
				}
				br.close();
				f3tainted = true;
				break;
		   }   
		}
	}
	
	
	System.out.println(fname3 + ": exists=" + f3.exists() + " canRead=" + f3.canRead() + " tainted=" + f3tainted);
	f3 = new File(fname3);

	String rurl = "";
	
	if (updateNcML.equals("true")) {
		if (archive == true) {
			if (debug.equals("true")) {
				out.println("<b>Errror.  Found previous archived response with same request id on server.  Responding with 400.</b>");out.flush();
			} else {
				response.sendError(400, "The requested data was previously requested with the same requestid.  Either use a different requestid or set updateNCML=false.");
				return;
			}
		}
	}

	if (f3.exists() && updateData.equals("false") && !f3tainted) {
		if (archive == true) {	
			rurl = dcurl + "/" + tsdsfedcDir + "/archive/" + dcsubdir + "/" + amd5 + ".out";

		} else {
			rurl = dcurl + "/" + tsdsfedcDir + "/queries/" + dcsubdir + "/" + amd5 + ".out";
		}

		// TODO: Do a HEAD request on rurl.  (In case file not be visible from http.)		
		// If not visible, try getting from syncsubmit.  If that fails, then stream file back.

		if (debug.equals("true")) {
			// Put something in the headers to indicate age if requestid was specified.
			// Also allow tsdsfe?requestid=ID to return information about the cache state for that
			// request id (if it was previously used or how old it is).
			if (stream.equals("true")) {
				out.println("<b>Found previous response on server.  Streaming:</b> " + fname3);out.flush();
			} else {
				out.println("<b>Found previous response on server.  Redirecting to: </b>" + "<a href='"+rurl+"'>"+rurl+"</a>");	out.flush();			
			}
		}

	} else {
		if (debug.equals("true")) {
			if (f3.exists()) {
				out.println("<b>updateData=true.  Removing previous response on server at: </b>" + fname3);out.flush();
			} else {
				out.println("<b>Did not find previous response on server at: </b>" + fname3);out.flush();				
			}
		}
		if (archive == true) {
			rurl = dcurl + "/" + tsdsfedcDir + "/archive/" + dcsubdir + "/" + amd5 + ".out";
		} else {
			rurl = durl;
		}
	}
	
	// If a requestid was given and local version not found, cache it.
	if ( ( stream.equals("true") || (archive == true) ) && (!f3.exists() || f3tainted || updateData.equals("true"))) {
		
		if (debug.equals("true")) {
			if (stream.equals("false")) {
				out.println("<b>Requestid specified along with a constraint and filter.  Archiving TSDS response using DataCache if first time URL has been requested. </b>");out.flush();
			}
			if (stream.equals("true")) {
				out.println("<b>Stream specified.  Caching TSDS response using DataCache if first time URL has been requested. </b>");out.flush();				
			}
		}

		poststr2 = "source=" + URLEncoder.encode(durl);
		if (archive == true) {
			poststr2 = "dir=" + tsdsfedcDir + "/archive/" + dcsubdir + "/" + "&" + poststr2;
		} else {
			poststr2 = "dir=" + tsdsfedcDir + "/queries/" + dcsubdir + "/" + "&" + poststr2;			
		}
		
		if (f3tainted || updateData.equals("true")) {
			poststr2 = poststr2 + "&forceWrite=true&forceUpdate=true";
		}
		URL url2 = new URL(dcserver);

		if (debug.equals("true")) {
			out.println("<b>-Posting to " + dcserver + ":</b> " + poststr2);out.flush();
		}

		URLConnection conn2 = url2.openConnection();
		conn2.setDoOutput(true);
		OutputStreamWriter wr2 = new OutputStreamWriter(conn2.getOutputStream());
		wr2.write(poststr2);
		wr2.flush();
		
		// Read and parse JSON response.
		InputStreamReader is2 = new InputStreamReader(conn2.getInputStream());
		java.util.Scanner s2 = new java.util.Scanner(is2).useDelimiter("\\A");
		String json2 = s2.hasNext() ? s2.next() : "";
		
		JSONArray ja2 = (JSONArray)JSONValue.parse(json2);
		JSONObject jo2 = (JSONObject)ja2.get(0);
	
		if (archive == true) {
			durla = dcurl + "/" + tsdsfedcDir + "/archive/" + dcsubdir + "/" + jo2.get("urlMd5")+".data";
		} else {
			durla = dcurl + "/" + dcsubdir + "/queries/" + tsdsfedcDir + "/" + jo2.get("urlMd5")+".data";
		}

		if (debug.equals("true")) {
			if (jo2.get("isFromCache").toString() == "true") {
				out.println("<b>Request was found in datacache cache. </b>");out.flush();
				if (!f3.exists()) {
					out.println("<b>... but request was not found at :</b> "+fname3 + " <b>Is "+dcdir+" not mounted?</b>");out.flush();
				} else {
					out.println("<b>... and request was found at :</b> "+fname3);out.flush();
				}
			} else {
				out.println("<b>Response from syncsubmit: </b>\n<textarea rows=10 style='width:100%'>" + json2 + "</textarea>");out.flush();		
			}
		}
		rurl = durla;
	}
		
	if (stream.equals("true")) {
		
			if (f3.exists()) {
				if (!debug.equals("true")) {			
					OutputStream r = response.getOutputStream();
					DataOutputStream os = new DataOutputStream(r);
				
					// TODO: Set this according to amd5.header. 
					response.setContentType("text/plain");
					if (attach.equals("true")) {
						response.setHeader("Content-Disposition","attachment; filename=" + amd5 );
					}
					
					// Read file from disk and stream
					InputStream in = new FileInputStream(fname3);
					IOUtils.copy(in,os);
					in.close();
					os.close();
					return;
				} else {
					out.println("<b>Streaming </b>: " + fname3);out.flush();					
				}
			} else { 
				if (!debug.equals("true")) {			
					OutputStream r = response.getOutputStream();
					DataOutputStream os = new DataOutputStream(r);
				
					// TODO: Set this according to amd5.header. 
					response.setContentType("text/plain");
					
					response.setHeader("Content-Disposition","attachment; filename=" + amd5 );

					//  Read file from HTTP and stream
					URL url3 = new URL(rurl);
					URLConnection conn3 = url3.openConnection();
					InputStreamReader is3 = new InputStreamReader(conn3.getInputStream());
					IOUtils.copy(is3,os);
					is3.close();
					return;
				} else {
					out.println("<b>Streaming: </b> <a href='"+rurl+"'>"+rurl+"</a>");out.flush();					
				}
		}

	} else {
		if (debug.equals("true")) {
			out.println("<b>Redirecting to: </b> <a href='"+rurl+"'>"+rurl+"</a>");out.flush();
		} else {
			response.setContentType("text/plain");
			response.sendRedirect(response.encodeRedirectURL(rurl));
		}
	}

	if (debug.equals("true")) {
		if (archive == true) {
			out.print("<a href='"+tsds+"/archive/" + qid + ".html'>Data query form</a> | ");out.flush();
		} else {
			out.print("<a href='"+tsds+"/queries/" + qid + ".html'>Data query form</a> | ");out.flush();
		}
		out.print("<a href=\""+durl+"\">Output</a> | ");
		//out.print("<a href='"+rurl+"'>Direct Link</a>");

	}
	if (debug.equals("true")) {
		out.println("</pre></body></html>");out.flush();
	}
//	}

%>
	