package ws;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class HttpRequestParser {
	private Properties config = new Properties();
	private String version = null;
	
	HttpResponse responseObj = null;
	private Hashtable<String,String> headerHash = new Hashtable<String,String>();
	private Socket socket = null;
	
	public HttpRequestParser(Socket socket) {
		try {
			InputStream in = null;
			try {
				File pfile = new File("./src/main/resources/config.properties");
				in = new FileInputStream(pfile);
				config.load(in);
				in.close();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
			
			//if we couldn't get the file from the file system, try the classloader
			if(in == null) {
				try {
					String filename = "main/resources/config.properties";
					ClassLoader cl = getClass().getClassLoader();
					URL res = Objects.requireNonNull(cl.getResource(filename),"Can't find configuration file " + filename);
					
					in = new FileInputStream(res.getFile());
					config.load(in);
					in.close();
				}
				catch(IOException ioex) {
					ioex.printStackTrace();
				}
			}
				
			if(config.containsKey("version"))
				version = config.getProperty("version");
			
			if(version == null)
				throw new Exception("Invalid configuration. Please update application configuration file");
		}
		catch(Exception e) {
			e.printStackTrace();
		}	
		
		this.socket = socket;
		responseObj = new HttpResponse(socket);
	}
	
	public void parseRequest() {
		InputStream is = null;
		Vector<String> headers = new Vector<String>();
		BufferedInputStream bis = null;
    	String metver = null;
    	try {    		 
    		/*From rfc2616: Certain buggy HTTP/1.0 client implementations generate extra CRLF's
	 		   after a POST request. To restate what is explicitly forbidden by the
	 		   BNF, an HTTP/1.1 client MUST NOT preface or follow a request with an
	 		   extra CRLF.
	 		   
	 		   this supports only 1.1. and will fail on preceding CRLFs
	 		*/
	     	
	     	//rfc2616: Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
    		is = socket.getInputStream();
    		bis = new BufferedInputStream(is, 4096);
	     	metver = stringifyBinaryLine(bis);
	     	metver = URLDecoder.decode(metver, "UTF-8"); //remove escaped characters
    	}
    	catch(IOException ee) {
    		ee.printStackTrace();
    		responseObj.sendResponse("400");
    		return;
    	}
    	
    	String method = null;
     	String location = null;
     	String httpver = null;
    	if(metver != null && metver.length() > 0) {
    		ArrayList<String> tokens = new ArrayList<String>();
    		StringTokenizer tokenizer = new StringTokenizer(metver, " ");
    	    while (tokenizer.hasMoreElements()) {
    	        tokens.add(tokenizer.nextToken());
    	    }
    	    
    	    try {
    	    	method = tokens.get(0);
	    		location = tokens.get(1);
	    		httpver = tokens.get(2);
	    		//there must be all three in a request
		    	if(method == null || location == null || httpver == null) {
		    		responseObj.sendResponse("400");
		    		return;
		    	}
		    	if(!httpver.equals(version)) {
		    		responseObj.sendResponse("505");
		    		return;
		    	}
    	    }
    	    catch(IndexOutOfBoundsException bound) {
    	    	responseObj.sendResponse("400");
    	    }
    	}
    	else {
    		responseObj.sendResponse("400"); //possibly a preceding CRLF in request
    		return;
    	}
    	
    	try {
	    	//parse headers
    		String headstr = stringifyBinaryLine(bis);
    		while(headstr != null && headstr.length() > 1) {
	    		headers.add(headstr);
	    		headstr = stringifyBinaryLine(bis);
	    	}
	    	if(headers.size() > 0)
	    		parseHeaders(headers);
	    	
	    	String decodeloc = URLDecoder.decode(location, "UTF-8").replaceAll(" ",""); //decode url, ie: remove escaped spaces and remove whitespace
	    	if(method.equalsIgnoreCase("GET")) {
		    	responseObj.getResponse(decodeloc);
		    	return;
		    }
		    else if (method.equalsIgnoreCase("PUT")) {
		    	responseObj.putResponse(decodeloc, bis);
	    		return;
		    }
		    else if (method.equalsIgnoreCase("HEAD")) {
		    	responseObj.headResponse(decodeloc);
		    	return;
		    }
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	finally {
    		try {
    			if(bis != null)
    				bis.close();
    		}
    		catch(IOException sigh) {
    			sigh.printStackTrace();
    		}
    	}
    	responseObj.sendResponse("505"); //we should never get here
    	return;
	}
	
	/*rfc2616: Each header field consists
   of a name followed by a colon (":") and the field value. Field names
   are case-insensitive. The field value MAY be preceded by any amount
   of LWS, though a single SP is preferred. Header fields can be
   extended over multiple lines by preceding each extra line with at
   least one SP or HT. Applications ought to follow "common form", where
   one is known or indicated, when generating HTTP constructs, since
   there might exist some implementations that fail to accept anything
   beyond the common forms.*/
	private void parseHeaders(Vector<String> requests) {
		for(String r : requests) {
			int colidx = r.indexOf(":");
			String key;
			if(colidx > 0)
				key = r.substring(0,colidx).trim(); //trim probably isn't needed here, but just in case.
			else
				key = r.trim();
			
			String value = "";
			if(colidx > 0)
				value = r.substring(colidx+1).trim();
			
			if(!headerHash.containsKey(key)) {
				headerHash.put(key,  value);
			}
			else { //more than one value for this header
				String alval = headerHash.get(key);
				StringBuffer sb = new StringBuffer();
				sb.append(alval);
				sb.append(",");
				sb.append(value);
				headerHash.put(key, sb.toString());
			}		
		}		
	}
	
	/*This is an attempt to recover from having to read character and binary data from the same stream
	 * TODO: There must be a library out there that handles this better*/
	private String stringifyBinaryLine(BufferedInputStream bis) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for(;;) {
			int ch = bis.read();
			if(ch == -1) //EOF
				break;

			//ignore returns, and break on new lines
			if(ch == '\r' || ch == '\n') {
				if(ch == '\r') {
					bis.read();
				}
				break;
			}
			baos.write(ch);
		}
		return baos.toString();
	}
}
