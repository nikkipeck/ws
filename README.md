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
Fix binary comparison test

Starting the server is relatively easy with the following lines:
SimpleServer ss = new SimpleServer(PORT);
ss.run();

or run ws.src.test.java.ws.SimpleServerStartTest

Some benchmarking done with apache bench:

ab -n 2000 -c 100 http://localhost:8081/IMG_0595.JPG (867 KB)
2.461 ms mean request time, 0 failed requests

ab -n 10000 -c 200 http://localhost:8081/apache-tomcat-8.5.32.exe (9.41 MB)
170.358 ms mean request time, 0 failed requests

ab -n 10000 -c 200 http://localhost:8081/strawberry-perl.msi (97 MB)
395.795 mean request time, 0 failed requests
