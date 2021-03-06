package ws;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/*Standalone that tests ServiceHandler construction*/

public class ServiceHandlerIntegrationTest {
	private static int PORT = 8081;
	private static SimpleServer ss;
	
	@BeforeAll
	static void startServer() {
		Thread serverThread = new Thread(() -> {
			ss = new SimpleServer(PORT);
			ss.run();
		});
		serverThread.start(); 
	}
	
	@Test
	void setTest() {
		try {
			Socket socket = new Socket("localhost", PORT);
			ServiceHandler sh = new ServiceHandler(socket);
			assertNotNull(sh);
			socket.close();
		}
		catch(IOException ioe) {
			fail(ioe.getMessage());
		}				
	}
}
