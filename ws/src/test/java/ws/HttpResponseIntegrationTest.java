package ws;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*This test class is an uncaught exception check. Standalone that tests character encoding, GET, PUT, and HEAD requests.
 Relies on files from resources: testfile.txt
 Writes putfile.html to configured location*/
public class HttpResponseIntegrationTest {
	
	private static int PORT = 8081;
	private static SimpleServer ss;
	
	@BeforeAll
	static void startServer() {
		Thread serverThread = new Thread() { 
			public void run() { 
				ss = new SimpleServer(PORT);  
                ss.run();
			}
		}; 
		serverThread.start(); 
	}
	
	Socket socket = null;
	HttpResponse httpr = null;
	BufferedOutputStream out = null;
	StatusCode codes = new StatusCode();
	
	@BeforeEach
	void setup() {
		try {
			socket = new Socket("localhost", PORT);
			
			out = new BufferedOutputStream(socket.getOutputStream());
			httpr = new HttpResponse(out);
		}
		catch(IOException ioe) {
			fail(ioe.getMessage());
		}
	}
	
	@Test
	void testEncoding() {
		String enc = "UTF-8";
		httpr.setEncoding(enc);
		String setEncoding = httpr.getEncoding();
		if(!setEncoding.equals(enc))
			fail("improper encoding " + setEncoding);
		
		enc = null;
		setEncoding = null;
		
		enc = "ISO-8859-1";
		httpr.setEncoding(enc);
		setEncoding = httpr.getEncoding();
		if(!setEncoding.equals(enc))
			fail("improper encoding " + setEncoding);
	}
	
	@Test
	void getTest() {
		try {
			httpr.getResponse("testFile.txt", "", false);
		}
		catch(Exception e) {
			fail(e);
		}
	}
	
	@Test
	void putTest() {
		try {
			httpr.putResponse("/putfile.html", new BufferedInputStream(socket.getInputStream()));
		}
		catch(Exception e) {
			fail(e);
		}
	}
	
	@Test
	void headTest() {
		try {
			httpr.headResponse("/testFile.txt", "");			
		}
		catch(Exception e) {
			fail(e);
		}
	}
	
	@Test
	void send500ResponseTest() {
		try {
			httpr.sendResponse(codes.getMessageFromCode("500"), false);
		}
		catch(Exception e) {
			fail(e);
		}
	}
	
	@AfterEach
	void tearDown() {
		if(socket != null && !socket.isClosed()) {
			try {
				socket.close();
			}
			catch(IOException ioe) {
				fail(ioe);
			}
		}
		httpr = null;		
	}
	
	@AfterAll
	static void stopServer() {
		ss.stop();
	}
}
