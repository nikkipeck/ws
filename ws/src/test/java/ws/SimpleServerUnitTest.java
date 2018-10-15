package ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SimpleServerUnitTest {
	@Test
	void testDefault() {
		//set up a server on the default port
		SimpleServer ss = new SimpleServer();
		assertNotNull(ss);
		
		assertEquals(8080, ss.getSocketPort());
	}
	
	@Test
	void testPort() {
		//set up a server on 8083
		SimpleServer ss = new SimpleServer(8083);
		assertNotNull(ss);
		
		assertEquals(8083, ss.getSocketPort());
	}
}

