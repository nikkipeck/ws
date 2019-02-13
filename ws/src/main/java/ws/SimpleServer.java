package ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class SimpleServer implements Runnable{
	private static Properties config = new Properties();
	private Thread runningThread;
	private static int default_port = -1;
	private static int max_threads = -1;
	private static int accept_timeout = 10000; //default to 10 second timeout
	private ServerSocket hsock;
	private ExecutorService servicer;
	private boolean run = true;
	
	/*This is a very simple logging implementation of a logging system. At some point it would be wise to move to an abstraction like Apache Commons or SL4J*/
	private final static Logger LOGGER = Logger.getLogger(SimpleServer.class.getName());
	private static Handler logFileHandler = null;
 	
	static {
		InputStream in = null;
		try {
			in = SimpleServer.class.getClassLoader().getResourceAsStream("config.properties");
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
			
			logFileHandler = new FileHandler(logdir + "simpleserver.log");
			LOGGER.addHandler(logFileHandler);
			logFileHandler.setLevel(Level.ALL);
			LOGGER.setLevel(Level.ALL);
			LOGGER.log(Level.INFO, "SimpleServer configuration information. Logging directory " + logdir);
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}		
		
		if(config.containsKey("default_port"))
			default_port = Integer.parseInt(config.getProperty("default_port"));
		if(config.containsKey("max_threads"))
			max_threads = Integer.parseInt(config.getProperty("max_threads"));
			
		if(default_port < 0 || max_threads < 0) {
			LOGGER.log(Level.CONFIG, "Invalid configuration for " + SimpleServer.class.getName() + " Please update application configuration file.");
			throw new ExceptionInInitializerError("Invalid configuration. Please update application configuration file");
		}
		
		if(config.containsKey("accept_timeout"))
			accept_timeout = Integer.parseInt(config.getProperty("accept_timeout"));
		
		//Informational logging
		
		LOGGER.log(Level.INFO, "SimpleServer configuration information. Port: " + default_port + " max threads: " + max_threads + " accept timeout: " + accept_timeout);
    }
	
	public SimpleServer() {
		this(-1);
	}	
	
	public SimpleServer(int port){
		int use_port = 0;
		if(port < 0)
			use_port = default_port;
		else
			use_port = port;
		
		try {
			hsock = new ServerSocket(use_port);
			hsock.setSoTimeout(accept_timeout); //blocking on accept has to end at some point.
			
			//Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
			servicer = Executors.newFixedThreadPool(max_threads);
		}
		catch(IOException ie) {
			LOGGER.logp(Level.SEVERE, this.getClass().getName(), ie.getStackTrace()[0].getMethodName(), "Error starting server" , (Throwable)ie);
			try {hsock.close();}
			catch(Exception e) {}
			shutdown(servicer);
		}
		catch(Exception e) {
			LOGGER.logp(Level.SEVERE, this.getClass().getName(), e.getStackTrace()[0].getMethodName(), "Error starting server", (Throwable)e);
		}
	}
	
	public void run() {
		synchronized(this) {
			runningThread = Thread.currentThread();
		}
		
		try {
			while(run && !servicer.isShutdown()) {
				Socket s = hsock.accept();
				s.setSoTimeout(accept_timeout);
				servicer.execute(new ServiceHandler(s));
			}
		}
		catch(Exception ee) {
			shutdown(servicer);
			LOGGER.logp(Level.WARNING, this.getClass().getName(), ee.getStackTrace()[0].getMethodName(), "Exception caught", (Throwable)ee);
		}
		finally {
			if(!servicer.isShutdown())
				servicer.shutdownNow(); //prevents waiting tasks from starting and attempts to stop currently executing tasks
			try {hsock.close();	}
			catch(IOException ie) {
				LOGGER.logp(Level.WARNING, this.getClass().getName(), ie.getStackTrace()[0].getMethodName(), "IOException caught", (Throwable)ie);
			}
		}
	}
	
	public int getSocketPort() {
		if(hsock != null)
			return hsock.getLocalPort();
		return -1;
	}
	
	public void shutdown(ExecutorService servicer) {
		if(servicer == null)
			return;
		
		if(hsock !=null && !hsock.isClosed()) {
			try {hsock.close();}
			catch(IOException ee) {
				LOGGER.logp(Level.WARNING, this.getClass().getName(), ee.getStackTrace()[0].getMethodName(), "IOException caught", (Throwable)ee);
			}
		}
		
		servicer.shutdown(); //try a graceful shutdown		
		try {
			if (!servicer.awaitTermination(45, TimeUnit.SECONDS)) {
				servicer.shutdownNow(); //really shut it down
				if(!servicer.awaitTermination(45, TimeUnit.SECONDS))
					LOGGER.logp(Level.WARNING, this.getClass().getName(), "", "Servicer did not shut down properly");					
		    }
		}
		catch(InterruptedException ie) {
			servicer.shutdownNow(); //one last try
			LOGGER.logp(Level.WARNING, this.getClass().getName(), ie.getStackTrace()[0].getMethodName(), "InterruptedException caught", (Throwable)ie);
		}
	}
	
	public void stop() {
		run = false;
		runningThread.interrupt();
	}
}
