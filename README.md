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

Future development:
Modularity for ease and completeness in testing

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

wsSystemTest - a full functionality test

SimpleServerUnitTest - test that a server is set up on the correct port, default or specified

HttpRequestParserUnitTest - requires SimpleServerStartTest to run, will test full parsing of http requests and responses

HttpResponseUnitTest - this is a basic uncaught excpetion test for the HttpResponse object

ServiceHandlerUnitTest - requres SimpleServerStartTest to run, will test ServiceHandler initialization

SimpleServerStartTest - will start the web server and handle requests from 3rd party programs (i.e. Postman), and from internal unit tests. 
