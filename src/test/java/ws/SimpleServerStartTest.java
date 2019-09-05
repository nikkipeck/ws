package ws;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.SocketTimeoutException;
/*Standalone test that will start a server on port 8081 and handle requests*/
public class SimpleServerStartTest {
	public static final int PORT = 8081;
	
	@Test
	public void requestTest() {
		try {
			SimpleServer ss = new SimpleServer(PORT);
			assertNotNull(ss);
			ss.run();
		}
		catch(ExceptionInInitializerError eiie) {
			fail(eiie);
		}
		catch(Exception any) {
			if(!(any instanceof SocketTimeoutException))
				fail(any);
		}
	}
}
