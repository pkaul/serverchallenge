package de.girino.serverchallenge;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Integration tests for server application. Starts server listening on a (random) http port
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerApplicationIT {

    /**
     * Date format of standard HTTP headers
     */
    private static final String LASTMODIFIED_DATEFORMAT = "E, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Example file path
     */
    private static final String TEXTFILE_PATH = "/example.txt";

    /**
     * Port where server is listening to
     */
    @LocalServerPort
    private int localServerPort;

    /**
     * Tests that an existing file is properly delivered including standard headers for request method HEAD
     */
    @Test
    public void testMethodHead() {

        RestAssured.given().
                expect().
                    statusCode(200).
                    header(HttpHeaders.LAST_MODIFIED, Matchers.notNullValue()).
                    header(HttpHeaders.ETAG, Matchers.notNullValue()).
                    contentType("text/plain").
                    body(Matchers.isEmptyOrNullString()).
                when().head(getServerBaseUrl() + TEXTFILE_PATH);
    }

    /**
     * Tests that an existing file is properly delivered including standard headers for request method GET
     */
    @Test
    public void testMethodGet() {

        RestAssured.given().
                expect().
                    statusCode(200).
                    header(HttpHeaders.CONTENT_LENGTH, Matchers.notNullValue()).
                    header(HttpHeaders.LAST_MODIFIED, Matchers.notNullValue()).
                    header(HttpHeaders.ETAG, Matchers.notNullValue()).
                    contentType("text/plain").
                    body(Matchers.not(Matchers.isEmptyOrNullString())).   // check that there is some content!
                when().get(getServerBaseUrl() + TEXTFILE_PATH);
    }

    /**
     * Tests that a non existing file is answered with 404
     */
    @Test
    public void testNotFound() {

        RestAssured.given().
                expect().response().statusCode(404).
                when().head(getServerBaseUrl() + "/files/this-file-does-not-exist.txt");
    }

    /**
     * Tests that "If-Modified-Since" is working correctly
     */
    @Test
    public void testIfModifiedSince() {

        DateTimeFormatter lastModifiedFormatter = DateTimeFormatter.ofPattern(LASTMODIFIED_DATEFORMAT).withLocale(Locale.US);

        // fetch "Last-Modified" header
        String lastModifiedString = RestAssured.given().get(getServerBaseUrl() + TEXTFILE_PATH).getHeader(HttpHeaders.LAST_MODIFIED);
        ZonedDateTime lastModified = ZonedDateTime.from(lastModifiedFormatter.parse(lastModifiedString));

        // 1.) Ask for resources "If-Modified-Since" == "Last-Modified". Expect status 304 "Not Modified"
        RestAssured.given().
                    header(HttpHeaders.IF_MODIFIED_SINCE, lastModifiedString).
                expect().
                    statusCode(304).
                    body(Matchers.isEmptyOrNullString()).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);

        // 2.) Ask for resources "If-Modified-Since" < "Last-Modified". Expect content to be fully delivered
        String dateBeforeLastModified = lastModified.minusHours(1).format(lastModifiedFormatter);
        RestAssured.given().
                    header(HttpHeaders.IF_MODIFIED_SINCE, dateBeforeLastModified).
                expect().
                    statusCode(200).
                    body(Matchers.not(Matchers.isEmptyOrNullString())).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);

        // 3.) Ask for resources "If-Modified-Since" > "Last-Modified". Expect status 304 "Not Modified"
        String dateAfterLastModified = lastModified.plusHours(1).format(lastModifiedFormatter);
        RestAssured.given().
                    header(HttpHeaders.IF_MODIFIED_SINCE, dateAfterLastModified).
                expect().
                    statusCode(304).
                    body(Matchers.isEmptyOrNullString()).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);
    }

    /**
     * Tests that ETag works with conditional requests
     */
    @Test
    public void testETag() {

        // fetch ETag
        String etag = RestAssured.given().get(getServerBaseUrl() + TEXTFILE_PATH).getHeader(HttpHeaders.ETAG);
        String otherETag = "00000000000000000000000000000000";


        // Fetches content with If-Match == etag. Expect content to be delivered fully.
        RestAssured.given().
                    header(HttpHeaders.IF_MATCH, etag).
                expect().
                    statusCode(200).
                    body(Matchers.not(Matchers.isEmptyOrNullString())).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);

        // Fetches content with If-None-Match == etag. Expect "not modified" response
        RestAssured.given().
                    header(HttpHeaders.IF_NONE_MATCH, etag).
                expect().
                    statusCode(304).
                    body(Matchers.isEmptyOrNullString()).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);

        // Fetches content with If-None-Match != etag. Expect content to be delivered fully.
        RestAssured.given().
                    header(HttpHeaders.IF_NONE_MATCH, otherETag).
                expect().
                    statusCode(200).
                    body(Matchers.not(Matchers.isEmptyOrNullString())).
                when().get(getServerBaseUrl() + TEXTFILE_PATH);

    }


    /**
     * Tests that relative paths can't be used to access files from outside base dir
     */
    @Test
    public void testInvalidPath() {

        RestAssured.given().
                expect().response().statusCode(400).
                when().get(getServerBaseUrl() + "/files/../../../application.yml");
    }


    /**
     * Tests that content types are delivered correctly for different file types
     */
    @Test
    public void testContentType() {

        RestAssured.given().expect().contentType("text/html").when().get(getServerBaseUrl() + "/example.html");
        RestAssured.given().expect().contentType("text/plain").when().get(getServerBaseUrl() + "/example.txt");
        RestAssured.given().expect().contentType("image/png").when().get(getServerBaseUrl() + "/image/example.png");
    }


    /**
     * Tests directory listing
     */
    @Test
    public void testDirectoryListing() {
        RestAssured.given().
                expect().
                    statusCode(200).
                    contentType("text/html").
                    body("/html/body/ul/li[0]/a", Matchers.notNullValue()).   // expect a link
                when().get(getServerBaseUrl() + "/image/");
    }

    // =========

    private String getServerBaseUrl() {
        return "http://localhost:" + localServerPort;
    }
}
