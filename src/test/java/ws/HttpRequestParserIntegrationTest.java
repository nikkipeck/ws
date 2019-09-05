package ws;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpRequestParserIntegrationTest {
	
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
	void testSingleGet() {
		try  (Socket clientSocket = new Socket("localhost",PORT)){
			PrintStream printer = new PrintStream(clientSocket.getOutputStream(), true); //autoflush
			//printer.println("GET /src/main/resources/testFile.txt HTTP/1.1"); //get file from path
			printer.println("GET /testFile.txt HTTP/1.1"); //get file from fileroot
			printer.println("If-None-Match: \"eowowief\",\"5B01F0F8DE85CFE5166F0F07DC09591A\"");
			printer.println("accept-charset: UTF-8");
			printer.println(); //properly formatted request will include an empty line after headers
			printer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
			String response = reader.readLine();
			if(!(response.equals("HTTP/1.1 200 OK") || response.equals("HTTP/1.1 304 Not Modified")))
				fail("invalid response " + response);			
		}
		catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	void testPut() {
		try (Socket clientSocket = new Socket("localhost",PORT)){
			PrintStream printer = new PrintStream(clientSocket.getOutputStream(), true); //autoflush
			printer.println("PUT /putfile7.html HTTP/1.1");
			printer.println("Content-Type: tex/html");
	        printer.println();
	        printer.println("<html>Test html for your day</html>");
	        printer.println();
	        printer.flush();
			printer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
			String response = reader.readLine();
			if(!(response.equals("HTTP/1.1 200 OK") || response.equals("HTTP/1.1 201 Created")))
				fail("invalid response " + response);			
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testHead() {
		try (Socket clientSocket = new Socket("localhost",PORT)){
			PrintStream printer = new PrintStream(clientSocket.getOutputStream(), true); //autoflush
			printer.println("HEAD /testFile.txt HTTP/1.1");
			printer.println("If-None-Match: \"eowowief\",\"5B01F0F8DE85CFE5166F0F07DC09591A\"");
			printer.println("accept-charset: UTF-8");
			printer.println(); //properly formatted request will include an empty line after headers
			printer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
			String response = reader.readLine();
			if(!(response.equals("HTTP/1.1 200 OK") || response.equals("HTTP/1.1 304 Not Modified")))
				fail("invalid response " + response);
			
			String contentType = reader.readLine();
			if(!contentType.contains("text/plain"))
				fail("Invalid content-type: " + contentType);
			
			String contentLength = reader.readLine();
			if(!contentLength.contains("53"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = reader.readLine();
			if(!etag.contains("FA912B338A597180CA8C79D4EC3404AC"))
				fail("Invalid ETag: " + etag);
			
			String connection = reader.readLine();
			if(connection == null || connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = reader.readLine();
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			String payload = reader.readLine();
			if(payload != null)
				fail("Payload should be null for HEAD request: " + payload);
		}
		catch(IOException e) {
			fail(e);
		}
	}
}
