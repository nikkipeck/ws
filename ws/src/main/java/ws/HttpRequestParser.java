package ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

public class HttpRequestParser {
	HttpResponse responseObj = new HttpResponse();
	private static final String GET_METHOD = "GET";
	private static final String PUT_METHOD = "PUT";
	
	public HttpRequestParser() {		
	}
	
	public String parseRequest(InputStream is) throws IOException{
		//TODO: check content-length to avoid extra large request issues?
		Vector<String> requests = new Vector<String>();
    	BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    	
    	while(br.ready()) {
    		String reqstr = br.readLine();
    		if(reqstr != null && !reqstr.equals(""))
    			requests.add(reqstr);
    	}
    	
    	String metver = requests.get(0);
    	if(metver != null && metver.length() > 0) {
    		String method = metver.substring(0, metver.indexOf(" "));
    		
	    	//request file section
	    	int fs = metver.indexOf(" ");
    		String fileloc = metver.substring(fs, metver.indexOf(" ", fs+1));
	    	
    		if(method == null)
	    		responseObj.respond401();
    		
	    	else if(method.equals(GET_METHOD)) {
	    		return responseObj.getResponse(fileloc);
	    	}
	    	else if (method.equals(PUT_METHOD)) {
	    		//TODO: this format is too rigid
	    		String content = requests.get(1);
	    		if(content.indexOf("text/html") < 0)
	    			return responseObj.respond400();
	        	
	    		String htmltag = "<html>";
	    		String closetag = "</html>";
	    		StringBuffer pb = new StringBuffer();
	    		boolean payload = false;
	    		for(String pl : requests) {
	    			if(pl.indexOf(htmltag) >= 0) { //this is payload
	    				payload = true;
	    				pb.append(pl);
	    				if(pl.indexOf(closetag) > 0) //it's a one-liner
	    					break;
	    			}
	    			else if(payload) {
	    				pb.append(pl);
	    				if(pl.indexOf(closetag) > 0) //we're done
	    					break;
	    			}	    				
	    		}
	    		
	    		if(pb.length() > 0)
	    			return responseObj.putResponse(fileloc, pb.toString());
	    		else
	    			return responseObj.respond400();
	    	}
	    	else {
	    		return responseObj.respond401();	    		
	    	}	    		
    	}
    	else
    		return responseObj.respond400();
    	return responseObj.respond401();
	}
}
