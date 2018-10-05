package ws;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

public class wsSystemTest {
	
	Socket clientSocket = null;
	String charset = "ISO-8859-1";
	
	@Test
	void testGet() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /getfile.txt HTTP/1.1");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);
			
			String contentType = br.readLine();
			if(!contentType.contains("text/plain"))
				fail("Invalid content-type: " + contentType);
			
			String contentLength = br.readLine();
			if(!contentLength.contains("67"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = br.readLine();
			if(!etag.contains("5B01F0F8DE85CFE5166F0F07DC09591A"))
				fail("Invalid ETag: " + etag);
			
			String connection = br.readLine();
			if(connection == null || connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = br.readLine();
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			String line1 = br.readLine();
			if(!line1.equals("This file was served from my simple web server."))
				fail("invalid get file line1 " + line1);

			String line2 = br.readLine();
			if(!line2.equals("Have a lovely day!"))
				fail("invalid get file line2 " + line2);
			
			String line3 = br.readLine();
			if(line3 != null)
				fail("invalid get file line3 " + line3);

	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testUnsupportedHttpVersion() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /getfile.txt HTTP/1.0");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 505 HTTP Version not supported"))
				fail("Unexpected response " + text);

	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void test304() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /src/main/resources/testFile.txt HTTP/1.1");
	        stream.println("If-None-Match: \"FA912B338A597180CA8C79D4EC3404AC\"");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 304 Not Modified"))
				fail("Unexpected response " + text);

	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	void testHTML() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /src/main/resources/testHtml.html HTTP/1.1");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);
			
			String contentType = br.readLine();
			if(!contentType.contains("text/html"))
				fail("Invalid content-type: " + contentType);
			
			String contentLength = br.readLine();
			if(!contentLength.contains("33"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = br.readLine();
			if(!etag.contains("AE942B0EE0847F85F2DD12DA26BD9B7C"))
				fail("Invalid ETag: " + etag);
			
			String connection = br.readLine();
			if(connection == null || connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = br.readLine();
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			String line1 = br.readLine();
			if(!line1.equals("<html><h1>Hello World</h1></html>"))
				fail("invalid get html file line1 " + line1);

			String line2 = br.readLine();
			if(line2 != null)
				fail("invalid get html file line2 " + line2);

	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testBinary() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /src/main/resources/testBin.bin HTTP/1.1");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);
			
			String header1 = br.readLine();
			System.out.println("content-type " + header1);
			
			String header2 = br.readLine();
			System.out.println("content-length " + header2);
			
			String etag = br.readLine();
			System.out.println("etag " + etag);
			
			String connection = br.readLine();
			System.out.println("connection " + connection);
			
			String emptyLine = br.readLine();
			System.out.println("emptyLine " + emptyLine);
			
			//TODO: this assertion has a number of problems. I'm not comparing byte data, but strings of byte data and only the first
			//string read from the file system.
			String payloadStr = br.readLine();
			System.out.println("bytes from server " + payloadStr);
			
			File fromFs = new File("./src/main/resources/testBin.bin");
			List<String> fls = Files.readAllLines(fromFs.toPath(), Charset.forName(charset));
			String fsPayload = fls.get(0);
			System.out.println("fsbytes " + fsPayload);
			
			assertEquals(payloadStr, fsPayload);
			
	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}

	
	@Test
	void testPut() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("PUT /putfile.html HTTP/1.1");
	        stream.println("Content-Type: tex/html");
	        stream.println();
	        stream.println("<html>Test html for your day</html>");
	        stream.println();
	        stream.flush();
        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));
	        
	        String text = br.readLine();
			if(!text.equals("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);

	        stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testHead() {
		try {
			clientSocket = new Socket("localhost", 8081);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("HEAD /src/main/resources/testFile.txt HTTP/1.1");
	        stream.println();
	        stream.flush();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), charset));

			String text = br.readLine();
			if(!text.equals("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);
			
			String contentType = br.readLine();
			if(!contentType.contains("text/plain"))
				fail("Invalid content-type: " + contentType);
			
			String contentLength = br.readLine();
			if(!contentLength.contains("53"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = br.readLine();
			if(!etag.contains("FA912B338A597180CA8C79D4EC3404AC"))
				fail("Invalid ETag: " + etag);
			
			String connection = br.readLine();
			if(connection == null || connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = br.readLine();
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			String payload = br.readLine();
			if(payload != null)
				fail("Payload should be null for HEAD request: " + payload);

			stream.close();
	        clientSocket.close();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}
