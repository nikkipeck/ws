# ws
simple java webserver

Allowed methods:
GET
HEAD
PUT

The server listens on http://localhost:8080 by default, but will also accept a different port on instantiation. (Allowable ports are between 0 and 65535, inclusive)

src.main.java.ws.SimpleServer.java
Creates a ServerSocket on a default (8080) or specified port. Runs an Excecutor service on socket.accept() which hands off parsing of http requests.

src.main.java.ws.HttpRequestParser.java
Parses headers and method requests. Delegates method handling to the HttpResponse object.

src.main.java.ws.HttpRequestResponse.java
Builds http responses to GET, HEAD, and PUT requests

src.main.java.ws.StatusCodes.java
Helper class to return the appropriate code and message in responses

Future development:
Implement SecurityManager
Modularity for ease and completeness in testing
