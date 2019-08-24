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

class HttpResponse {
	private ArrayList<String> headers = new ArrayList<>();
	private BufferedOutputStream socketOut;
	private File contentFile;
	
	private static Properties config = new Properties();
	private static String fileroot;
	private static String working;
	
	private String charset = "ISO-8859-1";
	
	private StatusCode codes = new StatusCode();
	
	/*This is a very simple logging implementation of a logging system. At some point it would be wise to move to an abstraction like Apache Commons or SL4J*/
	private final static Logger LOGGER = Logger.getLogger(SimpleServer.class.getName());

	static {
		try {
			InputStream in = HttpResponse.class.getClassLoader().getResourceAsStream("config.properties");
			if(in != null) {
				config.load(in);
				in.close();
			}
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

			Handler logFileHandler = new FileHandler(logdir + "httpResponse.log");
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
	
	HttpResponse(BufferedOutputStream socketOut) {
		this.socketOut = socketOut;
	}
	
	void setEncoding(String charset) {
		this.charset = charset;
	}
	
	String getEncoding() {
		return charset;
	}
	
	void headResponse(String fileloc, String match) {
		getResponse(fileloc, match, true);
	}
	
	void getResponse(String fileloc, String match, boolean headRequest) {
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
		}
		catch(Exception e) {
			LOGGER.logp(Level.WARNING, this.getClass().getName(), e.getStackTrace()[0].getMethodName(), "Exception caught", e);
			sendResponse("404", headRequest);
		}
	}
	
	void putResponse(String fileloc, BufferedInputStream is) {
		if(fileloc == null) {
			sendResponse("400", false);
			return;
		}
		
		String fileLocation;
		if(fileloc.startsWith("/") && fileloc.length() > 1) { //file
			if(working != null)
				fileLocation = working + fileloc;
			else
				fileLocation = "./src/main/resources/files" + fileloc;
		}
		else {
			sendResponse("400", false);
			return;
		}
		
		BufferedOutputStream out = null;
		boolean fileIsNew = false;
		try {
			File f = new File(fileLocation);
			if(!f.exists())
				fileIsNew = true;
			
			out = new BufferedOutputStream(new FileOutputStream(fileLocation));
			//is.transferTo(out); //TODO: this is blocking because it never gets an end of stream
			byte[] buffer = new byte[4 * 1024];
			while (is.available() > 0) {
				int len = is.read(buffer);
			    out.write(buffer, 0, len);
			}
			
			if(fileIsNew)
				sendResponse("201", false);
			else
				sendResponse("200", false);
		}
		catch(IOException ie) {
			LOGGER.logp(Level.WARNING, this.getClass().getName(), ie.getStackTrace()[0].getMethodName(), "IOException caught", ie);
			sendResponse("400", false);
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
			
	void sendResponse(String code, boolean headRequest) {
		StringBuilder respbuild = new StringBuilder();
		
		//rfc2616 Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		respbuild.append("HTTP/1.1 ").append(codes.getMessageFromCode(code)).append("\r\n");
		
		for(String r : headers) {
			respbuild.append(r);
		}
		
		respbuild.append("Connection: close\r\n"); //close connection
        respbuild.append("\r\n"); //end headers
        
        BufferedInputStream bufferedIn = null;
        try(PrintStream ps = new PrintStream(socketOut, true, charset)) {
	        ps.print(respbuild.toString()); //this contains headers
	        ps.flush();
	        
	        if(!code.equals("304") && !headRequest) { //do not send file in case of an unmodified response or a head request
		        if(contentFile != null && contentFile.length() > 0) {
		        	bufferedIn = new BufferedInputStream(new FileInputStream(contentFile), 4096);
			        bufferedIn.transferTo(ps);
		        }
	        }
	        
	        ps.flush();
	        socketOut.flush();
        }
        catch(IOException ioe) {
        	LOGGER.logp(Level.WARNING, this.getClass().getName(), ioe.getStackTrace()[0].getMethodName(), "IOException caught", ioe);
        }
        finally {
	        try {
		        if(socketOut != null)
		        	socketOut.close();
		        if(bufferedIn != null)
		        	bufferedIn.close();
	        }
	        catch(IOException psh) {
	        	LOGGER.logp(Level.WARNING, this.getClass().getName(), psh.getStackTrace()[0].getMethodName(), "Socket or InputStream failed to close on exception");
	        }
        }
	}
}
