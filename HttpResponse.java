package ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import javax.activation.MimetypesFileTypeMap;

public class HttpResponse {
	private Vector<String> headers = new Vector<String>();
	private StatusCode codes = new StatusCode();
	private Socket socket = null;
	private File contentFile = null;
	
	public HttpResponse(Socket socket) {
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
				modfileloc = "./src/main/resources/files" + fileloc;
			else if(fileloc.startsWith("/") && fileloc.length() == 1) //naked
				modfileloc = "./src/main/resources/files/getfile.txt";
			
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
	
	public void putResponse(String fileloc, BufferedReader br) {
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
		
		boolean fileIsNew = false;
		PrintWriter out = null;
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

			out = new PrintWriter(new FileWriter(modfileloc));
			String wrt = "";
			while(br.ready() && (wrt = br.readLine()) != null) {
				out.println(wrt);
				out.flush();
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
			if(out != null)
				out.close();
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
