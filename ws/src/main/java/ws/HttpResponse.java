package ws;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Vector;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.DatatypeConverter;

public class HttpResponse {
	private Vector<String> headers = new Vector<String>();
	private BufferedOutputStream socketOut = null;
	private File contentFile = null;
	
	private static Properties config = new Properties();
	private static String fileroot = null;
	
	private String charset = "ISO-8859-1";
	
	private StatusCode codes = new StatusCode();
	
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
			
		if(config.containsKey("fileroot"))
			fileroot = config.getProperty("fileroot");
		
		if(fileroot == null)
			throw new ExceptionInInitializerError("Invalid configuration. Please update application configuration file");
	
	}
	
	public HttpResponse(BufferedOutputStream socketOut) {		
		this.socketOut = socketOut;
	}
	
	public void setEncoding(String charset) {
		this.charset = charset;
	}
	
	public String getEncoding() {
		return charset;
	}
	
	public void getResponse(String fileloc, String match) {
		if(fileloc == null) {			
			sendResponse("400");
			return;
		}
		
		try {
			String fileLocation = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0)  //path
				fileLocation = "." + fileloc; //add a dot
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				fileLocation = fileroot + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				fileLocation = fileroot + "getfile.txt"; 
			
			if(fileLocation != null) {
				String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileLocation);
				
				byte[] b = Files.readAllBytes(Paths.get(fileLocation));
	        	byte[] hash = MessageDigest.getInstance("MD5").digest(b);
	        	String hex = DatatypeConverter.printHexBinary(hash); //convert to hex
	        	
		        headers.add("Content-Type: " + mimetype + "\r\n");
				headers.add("Content-Length: " + new File(fileLocation).length() + "\r\n");
				headers.add("ETag: " + hex + "\r\n");
				
				if(match != null && match.contains(hex)) {
					sendResponse("304");
					return;
				}
				
				contentFile = new File(fileLocation);

				sendResponse("200");
				return;
			}
			else {
				sendResponse("400");
				return;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			sendResponse("404");
		}
	}
	
	public void putResponse(String fileloc, BufferedInputStream is) {
		if(fileloc == null) {
			sendResponse("400");
			return;
		}
		
		String fileLocation = null;
		if(fileloc.startsWith("/") && fileloc.length() > 1) //file
			fileLocation = "./src/main/resources/files" + fileloc;
		else {
			sendResponse("400");
			return;
		}
		
		BufferedOutputStream out = null;
		boolean fileIsNew = false;
		try {
			if(fileLocation != null){
				File f = new File(fileLocation);
				if(!f.exists())
					fileIsNew = true;
			}
			
			out = new BufferedOutputStream(new FileOutputStream(fileLocation));
			//is.transferTo(out); //TODO: this is blocking because is never gets an end of stream
			byte[] buffer = new byte[4 * 1024];
			while (is.available() > 0) {
				int len = is.read(buffer);
			    out.write(buffer, 0, len);
			}
			
			if(fileIsNew)
				sendResponse("201");
			else
				sendResponse("200");
			return;
		}
		catch(IOException ie) {
			ie.printStackTrace();
			sendResponse("400");
		}
		finally {
			if(out != null) {
				try {out.close();}
				catch(Exception e) {}
			}
				
		}
	}
	
	public void headResponse(String fileloc, String match) {
		if(fileloc == null) {			
			sendResponse("400");
			return;
		}
		
		try {
			String fileLocation = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0) //path
				fileLocation = "." + fileloc; //add a dot
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				fileLocation = "./src/main/resources/files" + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				fileLocation = "./src/main/resources/files/getfile.txt";
			
			if(fileLocation != null) {
				String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileLocation);
				
				byte[] b = Files.readAllBytes(Paths.get(fileLocation));
	        	byte[] hash = MessageDigest.getInstance("MD5").digest(b);
	        	String hex = DatatypeConverter.printHexBinary(hash); //convert to hex
	        	
				headers.add("Content-Type: " + mimetype + "\r\n");
				headers.add("Content-Length: " + new File(fileLocation).length() + "\r\n");
				headers.add("ETag: " + hex + "\r\n");
				
				if(match != null && match.contains(hex)) {
					sendResponse("304");
					return;
				}
				
				/*rfc2616 The HEAD method is identical to GET except that the server MUST NOT
				   return a message-body in the response, so no body on this one*/
				sendResponse("200");
				return;
			}
			else {
				sendResponse("400");
				return;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			sendResponse("404");
		}
	}
		
	public void sendResponse(String code) {
		StringBuffer respbuff = new StringBuffer();
		
		//rfc2616 Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		respbuff.append("HTTP/1.1 " + codes.getMessageFromCode(code) + "\r\n");
		
		for(String r : headers) {
			respbuff.append(r);
		}
		
		respbuff.append("Connection: close\r\n"); //close connection
        respbuff.append("\r\n"); //end headers
        
        PrintStream ps = null;
        BufferedInputStream in = null;
        try {
	        ps = new PrintStream(socketOut, true, charset);
	        ps.print(respbuff.toString()); //this contains headers
	        ps.flush();
	        
	        if(!code.equals("304")) { //do not send file in case of an unmodified response
		        if(contentFile != null && contentFile.length() > 0) {
		        	in = new BufferedInputStream(new FileInputStream(contentFile), 4096);
			        in.transferTo(ps);
		        }
	        }
	        
	        ps.flush();
	        socketOut.flush();
        }
        catch(IOException ioe) {
        	ioe.printStackTrace();
        }
        finally {
        	if(ps != null)
        		ps.close();
	        try {
		        if(socketOut != null)
		        	socketOut.close();
		        if(in != null)
		        	in.close();
	        }
	        catch(IOException psh) {
	        	psh.printStackTrace();
	        }
        }
	}
}
