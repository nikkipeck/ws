package ws;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

import javax.activation.MimetypesFileTypeMap;

public class HttpResponse {
	private Vector<String> headers = new Vector<String>();
	private StatusCode codes = new StatusCode();
	private Socket socket = null;
	private File contentFile = null;
	
	private Properties config = new Properties();
	private String fileroot = null;
	
	public HttpResponse(Socket socket) {
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
				
			if(config.containsKey("fileroot"))
				fileroot = config.getProperty("fileroot");
			
			if(fileroot == null)
				throw new Exception("Invalid configuration. Please update application configuration file");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
				
		this.socket = socket;
	}
	
	public void getResponse(String fileloc) {
		if(fileloc == null) {			
			sendResponse("400");
			return;
		}
		
		try {
			String modfileloc = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0) //path
				modfileloc = "." + fileloc.substring(1); //trim first / and add a dot
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				modfileloc = fileroot + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				modfileloc = fileroot + "getfile.txt";
			
			if(modfileloc != null) {
				String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(modfileloc);
				headers.add("Content-Type: " + mimetype + "\r\n");
				headers.add("Content-Length: " + new File(modfileloc).length() + "\r\n");
				contentFile = new File(modfileloc);
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
		
		String modfileloc = null;
		if(fileloc.startsWith("/") && fileloc.length() > 1) //file
			modfileloc = "./src/main/resources/files" + fileloc;
		else {
			sendResponse("400");
			return;
		}
		
		BufferedOutputStream out = null;
		boolean fileIsNew = false;
		try {
			if(modfileloc != null){
				try
				{
					InputStream in = new FileInputStream(modfileloc);
					in.close();
				}
				catch(FileNotFoundException fnf) {
					fileIsNew = true;
				}
			}
			
			out = new BufferedOutputStream(new FileOutputStream(modfileloc));
			//is.transferTo(out); //TODO: this is blocking because is never gets an EOF
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
	
	public void headResponse(String fileloc) {
		if(fileloc == null) {			
			sendResponse("400");
			return;
		}
		
		try {
			String modfileloc = null;
			if (fileloc.startsWith("/") && fileloc.indexOf("/",1) > 0) //path
				modfileloc = "." + fileloc.substring(1); //trim first / and add a dot
			else if(fileloc.startsWith("/") && fileloc.length() > 1) //file
				modfileloc = "./src/main/resources/files" + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				modfileloc = "./src/main/resources/files/getfile.txt";
			
			if(modfileloc != null) {
				String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(modfileloc);
				headers.add("Content-Type: " + mimetype + "\r\n");
				headers.add("Content-Length: " + new File(modfileloc).length() + "\r\n");
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
        
        OutputStream out = null;
        PrintStream ps = null;
        FileInputStream in = null;
        try {
	        out = socket.getOutputStream();
	        ps = new PrintStream(out);
	        ps.print(respbuff.toString()); //this contains headers
	        
	        if(contentFile != null && contentFile.length() > 0) {
		        in = new FileInputStream(contentFile);
		        in.transferTo(ps);
	        }
	        
	        ps.flush();
	        ps.close();
	        
	        out.flush();
	        out.close();
        }
        catch(IOException ioe) {
        	ioe.printStackTrace();
        }
        finally {
        	if(ps != null)
        		ps.close();
	        try {
		        if(out != null)
		        	out.close();
		        if(in != null)
		        	in.close();
	        }
	        catch(IOException psh) {
	        	psh.printStackTrace();
	        }
        }
	}
}
