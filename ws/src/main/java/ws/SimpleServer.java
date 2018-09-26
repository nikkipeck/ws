package ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

//TODO: SecurityManager, set up security policy and checkaccept from host/port
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
		try {	
			int use_port = 0;
			if(port < 0)
				use_port = default_port;
			else
				use_port = port;
			
			hsock = new ServerSocket(use_port);
			//Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
			servicer = Executors.newFixedThreadPool(max_threads);
		}
		catch(IOException ie) {
			try {hsock.close();}
			catch(Exception e) {}
			ie.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			while(true) { //just run forever
				//Executes the given task sometime in the future.
				servicer.execute(new ServiceHandler(hsock.accept()));
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			servicer.shutdown(); //will execute previously submitted tasks before going away
		}
		finally {
			if(!servicer.isShutdown())
				servicer.shutdownNow(); //prevents waiting tasks from starting and attempts to stop currently executing tasks
			try {
				hsock.close();
			}
			catch(IOException ie) {
				ie.printStackTrace();
			}
		}
	}
}

class ServiceHandler implements Runnable{
	private final Socket socket;
	private final HttpRequestParser parser;
	ServiceHandler(Socket socket){ 
		this.socket = socket; 
		parser = new HttpRequestParser(socket);
	}
	
	public void run() {
		try {
		    parser.parseRequest();
		    socket.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
