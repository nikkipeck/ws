package ws;

import java.util.Hashtable;

public class StatusCode {
	public static Hashtable<String,String> statusCodes = new Hashtable<String,String>();

	public StatusCode() {
		statusCodes.put("100", "100 Continue");
		statusCodes.put("101","101 Switching Protocols");
		
		statusCodes.put("200","200 OK");
		statusCodes.put("201","201 Created");
		statusCodes.put("202","202 Accepted");
		statusCodes.put("203","203 Non-Authoritative Information");
		statusCodes.put("204","204 No Content");
		statusCodes.put("205","205 Reset Content");
		statusCodes.put("206","206 Partial Content");
		
		statusCodes.put("300","300 Multiple Choices");
		statusCodes.put("301","301 Moved Permanently");
		statusCodes.put("302","302 Found");
		statusCodes.put("303","303 See Other");
		statusCodes.put("304","304 Not Modified");
		statusCodes.put("305","305 Use Proxy");
		statusCodes.put("307","307 Temporary Redirect");
		
		statusCodes.put("400","400 Bad Request");
		statusCodes.put("401","401 Unauthorized");
		statusCodes.put("402","402 Payment Required");
		statusCodes.put("403","403 Forbidden");
		statusCodes.put("404","404 Not Found");
		statusCodes.put("405","405 Method Not Allowed");
		statusCodes.put("406","406 Not Acceptable");
		statusCodes.put("407","407 Proxy Authentication Required");
		statusCodes.put("408","408 Request Time-out");
		statusCodes.put("409","409 Conflict");
		statusCodes.put("410","410 Gone");
		statusCodes.put("411","411 Length Required");
		statusCodes.put("412","412 Precondition Failed");
		statusCodes.put("413","413 Request Entity Too Large");
		statusCodes.put("414","414 Request-URI Too Large");
		statusCodes.put("415","415 Unsupported Media Type");
		statusCodes.put("416","416 Requested range not satisfiable");
		statusCodes.put("417","417 Expectation Failed");
		
		statusCodes.put("500","500 Internal Server Error");
		statusCodes.put("501","501 Not Implemented");
		statusCodes.put("502","502 Bad Gateway");
		statusCodes.put("503","503 Service Unavailable");
		statusCodes.put("504","504 Gateway Time-out");
		statusCodes.put("505","505 HTTP Version not supported");		
	}
	
	public String getMessageFromCode(String code) {
		if(statusCodes.containsKey(code)) 
			return statusCodes.get(code);
		else
			return "undefined code";
	}
}
