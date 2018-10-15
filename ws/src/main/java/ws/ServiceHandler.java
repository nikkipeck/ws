package ws;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServiceHandler implements Runnable {
	private final Socket socket;
	private HttpRequestParser parser = null;
	ServiceHandler(Socket socket){ 
		this.socket = socket; 
		try {
			parser = new HttpRequestParser(new BufferedOutputStream(socket.getOutputStream(), 4096));
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			if(parser != null)
				parser.parseRequest(socket.getInputStream());
			socket.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
