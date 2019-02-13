package ws;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;

public class StatusCodeUnitTest {
	@Test
	void testMessages() {
		StatusCode sc = new StatusCode();
		
		String local200 = "200 OK";
		String msg200 = sc.getMessageFromCode("200");
		assertEquals(local200, msg200);
		
		String localUnd = "undefined code";
		String und = sc.getMessageFromCode("777");
		assertEquals(localUnd, und);
		
		String local400 = "400 Bad Request";
		String msg400 = sc.getMessageFromCode("400");
		assertEquals(local400, msg400);
	}
}
