package ws;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;

public class HttpResponse {
	private static final String VERSION = "HTTP/1.1";
	private Vector<String> headers = new Vector<String>();
	private Vector<String> body = new Vector<String>();
	private StatusCode codes = new StatusCode();
	//TODO: allowed, then implement more methods, SYNCHRONIZE
	
	public String getResponse(String fileloc) {
		headers.add(VERSION);
		if(fileloc == null) {
			headers.add(codes.getMessageFromCode("400"));
			return buildResponseString();
		}
		
		fileloc = fileloc.trim(); //trim off random whitespace
		fileloc = fileloc.replaceAll("%20", ""); //get rid of escaped spaces
			
		InputStream in = null;
		BufferedReader reader = null;
		
		try {
			String modfileloc = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0) //path
				modfileloc = "." + fileloc.substring(1); //trim first / and add a dot
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				modfileloc = "./src/main/resources/files" + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				modfileloc = "./src/main/resources/files/getfile.txt";
			
			synchronized(this){				
				if(modfileloc != null) {
					in = new FileInputStream(modfileloc);
					reader = new BufferedReader(new InputStreamReader(in));
					
					StringBuffer sb = new StringBuffer();
					String reqstr = reader.readLine();
					
			    	while(reqstr != null && !reqstr.equals("")) {
			    		sb.append(reqstr + "\r\n");
			    		reqstr = reader.readLine();
			    	}				
					
					headers.add(codes.getMessageFromCode("200"));
					headers.add("Content-Type: text/plain\r\n");
					headers.add("Cache-Control: no-cache\r\n");
					body.add(sb.toString());
				}
				else
					headers.add(codes.getMessageFromCode("400")); //TODO: is this reachable?
			} 
		}
		catch(Exception e) {
			e.printStackTrace();
			headers.add(codes.getMessageFromCode("404"));
		}
		finally{
			try {
				//cleanup
				if(reader != null)
		    		reader.close();
		    	if(in != null)
		    		in.close();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return buildResponseString();
	}
	
	public String putResponse(String fileloc, String payload) {
		headers.add(VERSION);
		if(fileloc == null) {
			headers.add(codes.getMessageFromCode("400"));
			return buildResponseString();
		}
		
		fileloc = fileloc.trim(); //trim off random whitespace
		fileloc = fileloc.replaceAll("%20", ""); //get rid of escaped spaces
		
		boolean fileIsNew = false;
		InputStream in = null;
		PrintWriter writer = null;
		FileWriter fw = null;
		
		try {
			String modfileloc = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0) { //path, not allowed
				headers.add(codes.getMessageFromCode("403"));
				return buildResponseString();
			}
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				modfileloc = "./src/main/resources/files" + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) { //naked, bad request
				headers.add(codes.getMessageFromCode("400"));
				return buildResponseString();
			}
			
			synchronized(this){				
				if(modfileloc != null) {
					try {
						in = new FileInputStream(modfileloc);
						in.close();
					}
					catch(FileNotFoundException fnf) {
						fileIsNew = true;
					}
					
					//write file
					fw = new FileWriter(modfileloc);
				    writer = new PrintWriter(fw);
				    writer.write(payload);
				    				
					if(fileIsNew)
						headers.add(codes.getMessageFromCode("201"));
					else
						headers.add(codes.getMessageFromCode("200"));
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			headers.add(codes.getMessageFromCode("404"));
		}
		finally{
			try {
				//cleanup
				if(fw != null)
					fw.close();
				if(writer != null)
		    		writer.close();
		    	if(in != null)
		    		in.close();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return buildResponseString();
	}
	
	public String respond401() {
		headers.add(VERSION);
		headers.add(codes.getMessageFromCode("401"));
		return buildResponseString();
	}
	
	public String respond400() {
		headers.add(VERSION);
		headers.add(codes.getMessageFromCode("400"));
		return buildResponseString();
	}
	
	public String respond200() {
		headers.add(VERSION);
		headers.add(codes.getMessageFromCode("200"));
		return buildResponseString();
	}
	
	private String buildResponseString() {
		StringBuffer respbuff = new StringBuffer();
		//version and status need to be one line
		for(String r : headers) {
			respbuff.append(r + " ");
		}
		
		respbuff.append("Connection: close\r\n"); //close connection
        respbuff.append("\r\n"); //end headers
        
        if(body.size() > 0) {
        	for(String b : body) {
	        	respbuff.append(b);
	        }
        }
		
		return respbuff.toString();
	}
}
