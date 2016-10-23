# Challenge: Implement a HTTP server that serves static files from a directory


> A multi-threaded http web server (with thread-pooling) implemented in Java. The server should serve static files from a (user specified) root directory. The server shall be delivered as an executable JAR file that can be started with a simple "java -jar your_server.jar" command. Any command line parameters shall be optional.
> 
> The server must at least handle http GET and HEAD requests. Requests that denote a directory should produce a listing of the files and subdirectories in the specified directory.
>
> Please include all java source files in your submission. In an additional file, give a high level overview describing your implementation approach and design considerations.
> 
> Extension 1: Add proper handling of HTTP ETag, If-Match, If-Non-Match, If-Modified-Since headers.
> 
> Extension 2: Add proper HTTP/1.1 keep-alive behavior to your implementation based on the http-client's capabilities exposed through its request headers.


## Design & Considerations
In order to solve this challenge, [Spring Boot](https://projects.spring.io/spring-boot/) with an underlying 
[Apache Tomcat](http://tomcat.apache.org/) servlet container have been used. These provide prefabricated solutions for
common HTTP server related requirements. Overall design consideration was to use as much existing and proven code as possible
in order to prevent re-inventing the wheel. 

Thus, my efforts for solving this challenge where mostly related to setting up and wiring standard components similar to
what is described in [Spring Boot's getting started](https://spring.io/guides/gs/spring-boot/).
Anyway, not all requirements couldn't be been solved using out-of-the-box code, such as the directory listing functionality. 
Therefore a few custom implementations needed to be created and incorporated into this application.

This is how individual requirements are solved

* _Multi-threaded http server with thread pooling_: Apache Tomcat is an enterprise-ready servlet engine that comes with out-of-the-box multi threading and thread pooling.
    See [tomcat documentation](http://tomcat.apache.org/tomcat-8.0-doc/config/executor.html) for more details.
* _Executable JAR file_: Spring Boot supports generating a single executable JAR file including all necessary dependencies. See below for how to build and run the JAR exactly.
* _GET and HEAD handling_: Spring Boot's in-built [ResourceHttpRequestHandler](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/resource/ResourceHttpRequestHandler.html) 
    is used for serving static files via HTTP from a configurable local directory while fulfilling the HTTP specification. 
    In addition, this handler internally makes use of in-memory caching of resources and is therefore most likely suitable for high performance (and low disk I/O) delivery. 
* _Directory listing_: This is done by a custom implementation of [ResourceResolver](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/resource/ResourceResolver.html) 
    interface (see [DirectoryListingResourceResolver](./src/main/java/de/girino/serverchallenge/DirectoryListingResourceResolver.java))
    where a directory is internally translated into a HTML page. In addition, a little tweak has been added to make sure 
    that `ResourceHttpRequestHandler` also accepts requests for the base directory itself. See [StaticFileController](./src/main/java/de/girino/serverchallenge/StaticFileController.java)
    for more details.
* _If-Modified-Since_: Proper handling of conditional requests based on modification time is already included in above mentioned `ResourceHttpRequestHandler`
* _ETag, If-Match and If-Non-Match_: This is handled by [ShallowEtagHeaderFilter](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/filter/ShallowEtagHeaderFilter.html). 
    Note that this implementation has some limitations (e.g. regarding memory usage and spec compliance) that are described in [StaticFileController](./src/main/java/de/girino/serverchallenge/StaticFileController.java) in more detail. 
* _HTTP/1.1 keep-alive_: Apache Tomcat supports both HTTP/1.0 and HTTP/1.1 fully and is therefore able to properly handle 
  keep-alive behaviour according to its specification. See [documentation](https://tomcat.apache.org/tomcat-8.0-doc/config/http.html#HTTP/1.1_and_HTTP/1.0_Support) for more details. 

Additional Assumptions
* The resulting server will be executed on local workstations only and will not be exposed to the internet. Thus, security (e.g. protecting resources) does not need to be considered here.
* Total size of all served files fits easily into JVM's heap space (a couple of MBs). No large files (such as videos) are going to be served by this implementation.

## Project Directory Layout

This project uses Maven's directory layout in general. Besides that, these files and directories are worth to be mentioned

* [src/main/java/de/girino/serverchallenge/](src/main/java/de/girino/serverchallenge/): Custom code for setting up the server
* [src/main/resources/META-INF/resources/examplefiles/](src/main/resources/META-INF/resources/examplefiles/): Example files that can be used for testing the server
* [src/test/java/de/girino/serverchallenge/ServerApplicationIT.java](src/test/java/de/girino/serverchallenge/ServerApplicationIT.java): Integration tests that prove that server is working is expected (for most requirements)


## Build & Run


### Prerequisite
- Java (JDK 8) is installed
- Maven is installed


### Build

Invoke Maven like 

    mvn clean install
		
Resulting artifact can be found at `target/server.jar`		

### Run

After building, the server can be started like

    java -jar target/server.jar --documentroot=file:./src/main/resources/META-INF/resources/examplefiles
or

    java -jar target/server.jar --documentroot=file:C:/absolute/path/to/project/src/main/resources/META-INF/resources/examplefiles

where `documentroot` option is a directory in the local filesystem. Note that prefix `file:` is required!

Files can be accessed via

    http://localhost:8080/