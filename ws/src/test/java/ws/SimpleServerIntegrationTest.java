package ws;

import static org.junit.jupiter.api.Assertions.*;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;

/*Standalone that will start a server on port 8080 and wait for timeout. It will start a server on 8083 and wait for timeout.*/

public class SimpleServerIntegrationTest {
	@Test
	void testDefault() {
		try {
			//set up a server on the default port
			SimpleServer ssd = new SimpleServer();
			assertNotNull(ssd);
			
			assertEquals(8080, ssd.getSocketPort());
			ssd.run(); //this blocks, but there is an accept timeout that will end it
		}
		catch(Exception any) {
			/*Any exception is a failure, except for a socket timeout. A socket timeout is how the server
			 * stops blocking on socket.accpets and goes away somewhat gracefully*/
			if(!(any instanceof SocketTimeoutException))
				fail(any);
		}
	}
	
	@Test
	void testPort() throws InterruptedException{
		try {
			//set up a server on 8083
			SimpleServer ss = new SimpleServer(8083);
			assertNotNull(ss);
			
			assertEquals(8083, ss.getSocketPort());
			ss.run(); //this blocks, but there is an accept timeout that will end it
		}
		catch(Exception any) {
			/*Any exception is a failure, except for a socket timeout. A socket timeout is how the server
			 * stops blocking on socket.accpets and goes away somewhat gracefully*/
			if(!(any instanceof SocketTimeoutException))
				fail(any);
		}
	}
}

