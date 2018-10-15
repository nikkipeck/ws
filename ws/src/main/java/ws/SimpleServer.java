package ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class SimpleServer implements Runnable{
	private static Properties config = new Properties();
	private static int default_port = -1;
	private static int max_threads = -1;
	private ServerSocket hsock;
	private ExecutorService servicer;
	
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
		
		if(config.containsKey("default_port"))
			default_port = Integer.parseInt(config.getProperty("default_port"));
		
		if(config.containsKey("max_threads"))
			max_threads = Integer.parseInt(config.getProperty("max_threads"));
			
		if(default_port < 0 || max_threads < 0)
			throw new ExceptionInInitializerError("Invalid configuration. Please update application configuration file"); 
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
			
			//Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
			servicer = Executors.newFixedThreadPool(max_threads);
		}
		catch(IOException ie) {
			try {hsock.close();}
			catch(Exception e) {}
			shutdown(servicer);
			ie.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			for(;;) {
				//Executes the given task sometime in the future.
				servicer.execute(new ServiceHandler(hsock.accept()));
			}
		}
		catch(Exception ee) {
			shutdown(servicer);
			ee.printStackTrace();
		}
		finally {
			if(!servicer.isShutdown())
				servicer.shutdownNow(); //prevents waiting tasks from starting and attempts to stop currently executing tasks
			try {hsock.close();	}
			catch(IOException ie) {ie.printStackTrace();}
		}
	}
	
	public int getSocketPort() {
		if(hsock != null)
			return hsock.getLocalPort();
		return -1;
	}
	
	public void shutdown(ExecutorService servicer) {
		if(!hsock.isClosed()) {
			try {hsock.close();}
			catch(IOException ee) {}
		}
		
		servicer.shutdown(); //try a graceful shutdown		
		try {
			if (!servicer.awaitTermination(45, TimeUnit.SECONDS)) {
				servicer.shutdownNow(); //really shut it down
				if(!servicer.awaitTermination(45, TimeUnit.SECONDS))
					System.err.println("Servicer did not end");
		    }
		}
		catch(InterruptedException ie) {
			servicer.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
