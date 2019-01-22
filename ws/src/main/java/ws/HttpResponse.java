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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.DatatypeConverter;

public class HttpResponse {
	private ArrayList<String> headers = new ArrayList<String>();
	private BufferedOutputStream socketOut = null;
	private File contentFile = null;
	private boolean headRequest = false;
	
	private static Properties config = new Properties();
	private static String fileroot = null;
	private static String working = null;
	
	private String charset = "ISO-8859-1";
	
	private StatusCode codes = new StatusCode();
	
	/*This is a very simple logging implementation of a logging system. At some point it would be wise to move to an abstraction like Apache Commons or SL4J*/
	private final static Logger LOGGER = Logger.getLogger(SimpleServer.class.getName());
	private static Handler logFileHandler = null;
	
	static {
		InputStream in = null;
		try {
			in = HttpResponse.class.getClassLoader().getResourceAsStream("config.properties");
			config.load(in);
			in.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		if(config.containsKey("workingdir"))
			working = config.getProperty("workingdir");
		
		try {
			String logdir = "";
			if(config.containsKey("logdir"))
				logdir = config.getProperty("logdir");
			
			logFileHandler = new FileHandler(logdir + "httpResponse.log");
			LOGGER.addHandler(logFileHandler);
			logFileHandler.setLevel(Level.ALL);
			LOGGER.setLevel(Level.ALL);
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		if(config.containsKey("fileroot"))
			fileroot = config.getProperty("fileroot");
		
		if(fileroot == null) {
			LOGGER.log(Level.CONFIG, "Invalid configuration for " + SimpleServer.class.getName() + " Please update application configuration file.");		
			throw new ExceptionInInitializerError("Invalid configuration. Please update application configuration file");
		}	
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
	
	public void headResponse(String fileloc, String match) {
		getResponse(fileloc, match, true);
	}
	
	public void getResponse(String fileloc, String match, boolean headRequest) {
		if(fileloc == null) {			
			sendResponse("400", headRequest);
			return;
		}
		
		try {
			//Let's make this OS independent
			Path path = Paths.get(fileloc);
			File tempFile = path.toFile();
			boolean isFile = tempFile.isFile();
			
			if(!isFile) { //not a valid path to a file
				path = Paths.get(fileroot, fileloc);
				tempFile = path.toFile();
				isFile = tempFile.isFile();
				if(!isFile) { //not a valid file under the configured file root
					path = Paths.get(fileroot,"getfile.txt");
					tempFile = path.toFile();
					isFile = tempFile.isFile();
					if(!isFile) { //not anything we know how to deal with
						sendResponse("400", headRequest);
						return;
					}
				}
			}
			tempFile = null;
			
			contentFile = path.toFile();
			String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(contentFile);
			
			byte[] b = Files.readAllBytes(path);
        	byte[] hash = MessageDigest.getInstance("MD5").digest(b);
        	String hex = DatatypeConverter.printHexBinary(hash); //convert to hex
        	
	        headers.add("Content-Type: " + mimetype + "\r\n");
			headers.add("Content-Length: " + contentFile.length() + "\r\n");
			headers.add("ETag: " + hex + "\r\n");
			
			LOGGER.log(Level.INFO, "HTTPResponse attempting to serve " + path.toString());
			
			if(match != null && match.contains(hex)) {
				sendResponse("304", headRequest);
				return;
			}
			
			sendResponse("200", headRequest);
			return;
		}
		catch(Exception e) {
			LOGGER.logp(Level.WARNING, this.getClass().getName(), e.getStackTrace()[0].getMethodName(), "Exception caught", (Throwable)e);
			sendResponse("404", headRequest);
		}
	}
	
	public void putResponse(String fileloc, BufferedInputStream is) {
		if(fileloc == null) {
			sendResponse("400", headRequest);
			return;
		}
		
		String fileLocation = null;
		if(fileloc.startsWith("/") && fileloc.length() > 1) { //file
			if(working != null)
				fileLocation = working + fileloc;
			else
				fileLocation = "./src/main/resources/files" + fileloc;
		}
		else {
			sendResponse("400", headRequest);
			return;
		}
		
		BufferedOutputStream out = null;
		boolean fileIsNew = false;
		try {
			File f = new File(fileLocation);
			if(!f.exists())
				fileIsNew = true;
			
			out = new BufferedOutputStream(new FileOutputStream(fileLocation));
			//is.transferTo(out); //TODO: this is blocking because is never gets an end of stream
			byte[] buffer = new byte[4 * 1024];
			while (is.available() > 0) {
				int len = is.read(buffer);
			    out.write(buffer, 0, len);
			}
			
			if(fileIsNew)
				sendResponse("201", headRequest);
			else
				sendResponse("200", headRequest);
			return;
		}
		catch(IOException ie) {
			LOGGER.logp(Level.WARNING, this.getClass().getName(), ie.getStackTrace()[0].getMethodName(), "IOException caught", (Throwable)ie);
			sendResponse("400", headRequest);
		}
		finally {
			if(out != null) {
				try {out.close();}
				catch(Exception e) {
					LOGGER.logp(Level.WARNING, this.getClass().getName(), e.getStackTrace()[0].getMethodName(), "BufferedOutputStream failed to close on exception");
				}
			}
				
		}
	}
			
	public void sendResponse(String code, boolean headRequest) {
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
	        
	        if(!code.equals("304") && !headRequest) { //do not send file in case of an unmodified response or a head request
		        if(contentFile != null && contentFile.length() > 0) {
		        	in = new BufferedInputStream(new FileInputStream(contentFile), 4096);
			        in.transferTo(ps);
		        }
	        }
	        
	        ps.flush();
	        socketOut.flush();
        }
        catch(IOException ioe) {
        	LOGGER.logp(Level.WARNING, this.getClass().getName(), ioe.getStackTrace()[0].getMethodName(), "IOException caught", (Throwable)ioe);
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
	        	LOGGER.logp(Level.WARNING, this.getClass().getName(), psh.getStackTrace()[0].getMethodName(), "Socket or InputStream failed to close on exception");
	        }
        }
	}
}
