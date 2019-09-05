package ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleServerStarter {
	private static Properties config = new Properties();
	/*This is a very simple logging implementation of a logging system. At some point it would be wise to move to an abstraction like Apache Commons or SL4J*/
	private final static Logger LOGGER = Logger.getLogger(SimpleServerStarter.class.getName());
	private static Handler logFileHandler = null;
	
	public static void main(String[] args) {
		InputStream in = null;
		try {
			in = SimpleServerStarter.class.getClassLoader().getResourceAsStream("config.properties");
			config.load(in);
			in.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		try {
			String logdir = "";
			if(config.containsKey("logdir"))
				logdir = config.getProperty("logdir");
			
			logFileHandler = new FileHandler(logdir + "serverStarter.log");
			LOGGER.addHandler(logFileHandler);
			logFileHandler.setLevel(Level.ALL);
			LOGGER.setLevel(Level.ALL);
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		SimpleServer simple = new SimpleServer();
		LOGGER.info("SimpleServer running");
		simple.run();		
	}
}
