package ws;

import org.junit.jupiter.api.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SimpleServerTest {
	public static final int PORT = 8081;
	
	@Test
	public void requestTest() {
		SimpleServer ss = new SimpleServer(PORT);
		ss.run();
	}
}
