package ws;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
			eiie.printStackTrace();
		}
	}
}
