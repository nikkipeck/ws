package ws;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/*Standalone that will test GET, PUT, and HEAD requests. Also checks unsupported Http verson, 304 Not Modified, 
HTML data, and Binary data. 
Relies on files from resources folder: getfile.txt, testfile.txt, testHtml.html, and testBin.bin
Writes file putfile.html to configured location*/

public class WSSystemIntegrationTest {
	
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
	
	Socket clientSocket = null;
	String charset = "ISO-8859-1";

	@Test
	void testGet() {
		try {
			clientSocket = new Socket("localhost", PORT);
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
			if(!contentLength.contains("163"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = br.readLine();
			if(!etag.contains("651D879B87F41F1268E0EF81D2357ECE"))
				fail("Invalid ETag: " + etag);
			
			String connection = br.readLine();
			if(connection == null || connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = br.readLine();
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			String line1 = br.readLine();
			if(!line1.startsWith("This file was served from my simple web server."))
				fail("invalid get file line1 " + line1);
			
			String line2 = br.readLine();
			if(!line2.equals("It is a generic file, if you requested a different file there was an error. Please try again."))
				fail("invalid get file line3 " + line2);

			String line3 = br.readLine();
			if(!line3.equals("Have a lovely day!"))
				fail("invalid get file line3 " + line3);
			
			String line4 = br.readLine();
			if(line4 != null)
				fail("invalid get file line4 " + line4);

	        stream.close();
	        clientSocket.close();
		}
		catch(IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testUnsupportedHttpVersion() {
		try {
			clientSocket = new Socket("localhost", PORT);
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
			clientSocket = new Socket("localhost", PORT);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /testFile.txt HTTP/1.1");
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
			clientSocket = new Socket("localhost", PORT);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /testHtml.html HTTP/1.1");
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
		catch(IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testBinary() {
		try {
			clientSocket = new Socket("localhost", PORT);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("GET /testBin.bin HTTP/1.1");
	        stream.println();
	        stream.flush();
	        
	        BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream(), 4096);
			String text = stringifyBinaryLine(bis);
			if(!text.startsWith("HTTP/1.1 200 OK"))
				fail("Unexpected response " + text);
			
			String contentType = stringifyBinaryLine(bis);
			if(!contentType.contains("application/octet-stream"))
				fail("Invalid content-type: " + contentType);
			
			String contentLength =stringifyBinaryLine(bis);
			if(!contentLength.contains("2048"))
				fail("Invalid content-length: " + contentLength);
			
			String etag = stringifyBinaryLine(bis);
			if(!etag.contains("0D3E6A7A4E6B167967802A7D5F908C9B"))
				fail("Invalid ETag: " + etag);
			
			String connection = stringifyBinaryLine(bis);
			if(connection.equals(""))
				fail("Empty connection header");
			
			String emptyLine = stringifyBinaryLine(bis);
			if(!emptyLine.equals(""))
				fail("Malformatted response, line shoud be empty: " + emptyLine);
			
			//go get the payload
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for(;;) {
				int ch = bis.read();
				if(ch == -1) //EOF
					break;
				baos.write(ch);				
			}
			
			//go get the file root
			InputStream in = null;
			Properties config = new Properties();
			try {
				in = SimpleServer.class.getClassLoader().getResourceAsStream("config.properties");
				config.load(in);
				in.close();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
			
			String fileroot = "";
			if(config.containsKey("fileroot"))
				fileroot = config.getProperty("fileroot");

			byte[] payload = baos.toByteArray();
			
			File fromFs = new File(fileroot + "/testBin.bin");
			byte[] bs = Files.readAllBytes(fromFs.toPath());
			
			assertTrue(Arrays.equals(payload, bs));
			
	        stream.close();
	        clientSocket.close();
		}
		catch(IOException e) {
			fail(e.getMessage());
		}
	}

	
	@Test
	void testPut() {
		try {
			clientSocket = new Socket("localhost", PORT);
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
			clientSocket = new Socket("localhost", PORT);
			PrintStream stream = new PrintStream(clientSocket.getOutputStream(), true);
	        stream.println("HEAD /testFile.txt HTTP/1.1");
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
		catch(IOException e) {
			fail(e.getMessage());
		}
	}
	
	private String stringifyBinaryLine(BufferedInputStream bis) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for(;;) {
			int ch = bis.read();
			if(ch == -1) //EOF
				break;

			//ignore returns, and break on new lines
			if(ch == '\r' || ch == '\n') {
				if(ch == '\r') {
					bis.read();
				}
				break;
			}
			baos.write(ch);
		}
		return baos.toString(charset);
	}
	
	@AfterAll
	static void stopServer() {
		ss.stop();
	}
}
