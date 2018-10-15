package ws;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.Test;

public class ServiceHandlerUnitTest {
	@Test
	void setTest() {
		try {
			Socket socket = new Socket("localhost", 8081);
			ServiceHandler sh = new ServiceHandler(socket);
			assertNotNull(sh);
			socket.close();
		}
		catch(IOException ioe) {
			fail(ioe.getMessage());
		}
				
	}
}
