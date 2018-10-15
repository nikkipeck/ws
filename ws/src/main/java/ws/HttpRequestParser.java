package ws;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import java.nio.charset.Charset;

public class HttpRequestParser {
	private static Properties config = new Properties();
	private static String version = null;
	
	HttpResponse responseObj = null;
	private Hashtable<String,String> headerHash = new Hashtable<String,String>();
	private String charset = "ISO-8859-1";
	
	static {
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
			
		if(config.containsKey("version"))
			version = config.getProperty("version");
		
		if(version == null)
			throw new ExceptionInInitializerError("Invalid configuration. Please update application configuration file");
	}
	
	public HttpRequestParser(BufferedOutputStream out) {
		responseObj = new HttpResponse(out);
	}
	
	public void parseRequest(InputStream is) {
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
    		bis = new BufferedInputStream(is, 4096);
	     	metver = stringifyBinaryLine(bis);
	     	metver = URLDecoder.decode(metver, charset); //remove escaped characters
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
	    		//System.out.println("method " + method + " location " + location + " version " + httpver); //useful debug line

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
	    	
	    	String decodeloc = URLDecoder.decode(location, charset).replaceAll(" ",""); //decode url, ie: remove escaped spaces and remove whitespace
	    	if(method.equalsIgnoreCase("GET")) {
	    		//check headerHash for if-none-match
	    		String match = null;
	    		if(headerHash.containsKey("If-None-Match"))
	    			match = headerHash.get("If-None-Match");
	    		
		    	responseObj.getResponse(decodeloc, match);
		    	return;
		    }
		    else if (method.equalsIgnoreCase("PUT")) {
		    	responseObj.putResponse(decodeloc, bis);
	    		return;
		    }
		    else if (method.equalsIgnoreCase("HEAD")) {
		    	//check headerHash for if-none-match
		    	String match = null;
	    		if(headerHash.containsKey("If-None-Match"))
	    			match = headerHash.get("If-None-Match");
	    		
		    	responseObj.headResponse(decodeloc, match);
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
	   beyond the common forms.
	   Also RFC2616 HTTP/1.1 recipients MUST respect the charset label provided by the sender; and those 
		user agents that have a provision to "guess" a charset MUST use the charset from the
		 content-type field if they support that charset, rather than the
		 recipient's preference, when initially displaying a document. See
		 section 3.7.1.*/
	private void parseHeaders(Vector<String> requests) {
		String encodingKey = "accept-charset";
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
			
			if(key.equalsIgnoreCase(encodingKey)) {
				setCharset(value);
				responseObj.setEncoding(charset);
			}
			
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
		return baos.toString(charset);
	}
	
	//TODO: again, must be a library to do this
	private void setCharset(String charsetHeader) {
		/*rfc2616 Accept-Charset = "Accept-Charset" ":"1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
		* matches every character set (including ISO-8859-1) which is not mentioned elsewhere in accept-charset field
		If no "*" is present in an Accept-Charset field, then all character sets not explicitly mentioned get a quality value of 0, except for ISO-8859-1, which gets
		a quality value of 1 if not explicitly mentioned.
		Accept-Charset: iso-8859-5, unicode-1-1;q=0.8*/
		if(charsetHeader.indexOf("*") > 0) 
			return; //using ISO-8859-1, ignoring quality value(s)
		
		if(charsetHeader.indexOf(",") > 0){ //multiple possible charsets
			Double hiq = -1.0;
			String hics = null;
			StringTokenizer chop = new StringTokenizer(charsetHeader,",");
			while (chop.hasMoreElements()) {
    	        String cs = chop.nextToken();
    	        int semi = cs.indexOf(";"); //quality score
    	        if(semi > 0) {
    	        	String c = cs.substring(0, semi);
    	        	String q = cs.substring(semi+1);
    	        	try {
    	        		int idx = q.indexOf("=");
    	        		if(idx > 0) {
    	        			String subq = q.substring(idx+1);
    	        			Double qv = Double.parseDouble(subq);
		    	        	
		    	        	if(qv > hiq && Charset.isSupported(cs)) {
		    	        		hiq = qv;
		    	        		hics = c; //this is the highest quality charset so far
		    	        	}
    	        		}
    	        	}
    	        	catch(Exception e) {
    	        		e.printStackTrace();
    	        		responseObj.sendResponse("406"); //according to rfc2616
    	        	}
	    	    }
    	        else { //no quality score, defaults to 1
    	        	if(Charset.isSupported(cs)) {
    	        		hiq = 1.0;
    	        		hics = cs;
    	        	}
    	        }
    	    }
    	    if(hics == null)
    	    	responseObj.sendResponse("406"); //according to rfc2616
    	    else
    	    	charset = hics;			
		}
		else if(!Charset.isSupported(charsetHeader))
			responseObj.sendResponse("406"); //according to rfc2616
		else
			charset = charsetHeader; //charsetHeader is one value and is supported
		
	}
}
