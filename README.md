# ws
simple java webserver

*

Allowed methods:
GET
HEAD
PUT

The server listens on http://localhost:8080 by default, but will also accept a different port on instantiation. (Allowable ports are between 0 and 65535, inclusive)

*

src.main.java.ws.SimpleServerStarter.java
Main class that will start a SimpleServer instance

src.main.java.ws.SimpleServer.java
Creates a ServerSocket on a default (8080) or specified port. Runs an Excecutor service on socket.accept() which hands off parsing of http requests.

src.main.java.ws.HttpRequestParser.java
Parses headers and method requests. Delegates method handling to the HttpResponse object.

src.main.java.ws.HttpRequestResponse.java
Builds http responses to GET, HEAD, and PUT requests

src.main.java.ws.StatusCodes.java
Helper class to return the appropriate code and message in responses

*
Logging:
Logging is accomplished with a simple java.util.logging.Logger implementation. Log files are written to a directory specified in config.properties. 
*

Future development:
Modularity for ease and completeness in testing
Sustainable logging implementation (Apache Commons, or Log4J)
test suite, which will start a server instance and cycle through all of the unit tests

*

The server can be started by the following classes:
ws.src.main.java.ws.SimplerServerStarter (Main class)
ws.src.test.java.ws.SimpleServerStartTest (Test class)

*

Some benchmarking done with apache bench:

ab -n 2000 -c 100 http://localhost:8081/IMG_0595.JPG (867 KB)
2.461 ms mean request time, 0 failed requests

ab -n 10000 -c 200 http://localhost:8081/apache-tomcat-8.5.32.exe (9.41 MB)
170.358 ms mean request time, 0 failed requests

ab -n 10000 -c 200 http://localhost:8081/strawberry-perl.msi (97 MB)
395.795 mean request time, 0 failed requests

*

Testing:
All test files are located in /src/test/java/ws

wsSystemIntegrationTest - Starts a server on port 8081, tests GET, PUT, and HEAD Requests. Also tests unsupported Http versions, 304 Not modified responses, HTML data, and binary data. Relies on resource files: getfile.txt, testfile.txt, testHtml.html, testBin.bin. Writes putfile.html to configured location.

SimpleServerIntegrationTest - Starts a server on port 8080 and waits for timeout. Starts a server on port 8083 and waits for timeout.

HttpRequestParserIntegrationTest - Starts a server on port 8081, tests GET, PUT, and HEAD requests. Relies on resource files: testfile.txt. Writes putfile7.html to configured location.

HttpResponseIntegrationTest - this is a basic uncaught excpetion test for the HttpResponse object. Starts a server on port 8081, tess encoding, GET, PUT, and HEAD requests. Relies on resource files: testfile.txt. Writes putfile.html to configured location.

ServiceHandlerIntegrationTest - tests construction of ServiceHandler object

SimpleServerStartTest - Starts a server on port 8081, can handle requests. Will timeout after configured accept_timeout in milliseconds.

StatusCodeUnitTest - test status codes object.
