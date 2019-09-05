package ws;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceHandler implements Runnable {
	private final Socket handlerSocket;
	private HttpRequestParser parser;
	
	/*This is a very simple logging implementation of a logging system. At some point it would be wise to move to an abstraction like Apache Commons or SL4J*/
	private final static Logger LOGGER = Logger.getLogger(SimpleServer.class.getName());

	ServiceHandler(Socket socket){
		Properties config = new Properties();
		try {
			InputStream in = ServiceHandler.class.getClassLoader().getResourceAsStream("config.properties");
			if(in != null) {
				config.load(in);
				in.close();
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		try {			
			String logdir = "";
			if(config.containsKey("logdir"))
				logdir = config.getProperty("logdir");
			
			Handler logFileHandler = new FileHandler(logdir + "serviceHandler.log");
			LOGGER.addHandler(logFileHandler);
			logFileHandler.setLevel(Level.ALL);
			LOGGER.setLevel(Level.ALL);
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}

		handlerSocket = socket;
		try {
			parser = new HttpRequestParser(new BufferedOutputStream(handlerSocket.getOutputStream(), 4096));
		}
		catch(IOException e) {
			LOGGER.logp(Level.SEVERE, this.getClass().getName(), e.getStackTrace()[0].getMethodName(), "IOException caught", e);
		}
	}
	
	public void run() {
		try {
			if(parser != null)
				parser.parseRequest(handlerSocket.getInputStream()); //this socket is closed before this call. How?
		}
		catch(IOException ioe) {
			LOGGER.logp(Level.WARNING, this.getClass().getName(), ioe.getStackTrace()[0].getMethodName(), "IOException caught", ioe);
		}
	}
}
