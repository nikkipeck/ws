package ws;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

//TODO: SecurityManager, set up security policy and checkaccept from host/port
public class SimpleServer implements Runnable{
	//TODO: move these to properties file
	private static final int DEFAULT_PORT = 8080;
	private static final int MAX_THREADS = 4;
	private ServerSocket hsock;
	private ExecutorService servicer;
	
	public SimpleServer() {
		this(DEFAULT_PORT);
	}
	
	public SimpleServer(int port){
		try {
			hsock = new ServerSocket(port);
			//Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
			servicer = Executors.newFixedThreadPool(MAX_THREADS);
		}
		catch(IOException ie) {
			try {hsock.close();}
			catch(Exception e) {}
			ie.printStackTrace();
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
