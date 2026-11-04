package com.cabride.ride_service.Client;


import com.cabride.ride_service.Exception.RoutingException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OsrmRoutingProviderTest {

    private MockWebServer mockWebServer;
    private OsrmRoutingProvider routingProvider;

    @BeforeEach
    void setUp() throws IOException {

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure the provider to call the local mock server.
        routingProvider = new OsrmRoutingProvider(
                WebClient.builder()
        );

        // Inject the mock server URL into the provider.
        org.springframework.test.util.ReflectionTestUtils.setField(
                routingProvider,
                "baseUrl",
                mockWebServer.url("/").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void calculateRoute_shouldReturnRouteDetails() {

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "code": "Ok",
                          "routes": [
                            {
                              "distance": 12500.5,
                              "duration": 900.0,
                              "geometry": "encodedPolyline"
                            }
                          ]
                        }
                        """));

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        RouteResult result = routingProvider.calculateRoute(coordinates);

        assertEquals(12500.5, result.getDistanceMeters());
        assertEquals(900.0, result.getDurationSeconds());
        assertEquals("encodedPolyline", result.getGeometry());
    }

    @Test
    void calculateRoute_shouldRejectInsufficientCoordinates() {

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> routingProvider.calculateRoute(coordinates)
        );
    }

    @Test
    void calculateRoute_shouldRejectEmptyCoordinateList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> routingProvider.calculateRoute(List.of())
        );
    }

    @Test
    void calculateRoute_shouldThrowWhenOsrmReturnsNoRoute() {

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "code": "NoRoute",
                      "routes": []
                    }
                    """));

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(coordinates)
        );
    }
    @Test
    void calculateRoute_shouldThrowWhenOsrmReturnsHttpError() {

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(coordinates)
        );
    }

    @Test
    void calculateRoute_shouldConstructCorrectCoordinatePath()
            throws Exception {

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "code": "Ok",
                      "routes": [
                        {
                          "distance": 1000.0,
                          "duration": 120.0,
                          "geometry": "testGeometry"
                        }
                      ]
                    }
                    """));

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        routingProvider.calculateRoute(coordinates);

        var recordedRequest = mockWebServer.takeRequest();

        String requestPath = recordedRequest.getPath();

        assertNotNull(requestPath);
        assertTrue(requestPath.startsWith("/route/v1/driving/"));

        String decodedPath = java.net.URLDecoder.decode(
                requestPath,
                java.nio.charset.StandardCharsets.UTF_8
        );

        assertTrue(
                decodedPath.contains("80.3319,26.4499;80.2329,26.5123"),
                "OSRM must receive coordinates in longitude,latitude order"
        );

        assertTrue(
                requestPath.contains(
                        "80.3319%2C26.4499%3B80.2329%2C26.5123"
                ),
                "OSRM must receive coordinates in longitude,latitude order"
        );
    }
    @Test
    void calculateRoute_shouldThrowWhenResponseTimeoutOccurs() {

        // Simulate an OSRM server that responds after 7 seconds.
        mockWebServer.enqueue(new MockResponse()
                .setHeadersDelay(7, java.util.concurrent.TimeUnit.SECONDS)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "code": "Ok",
                      "routes": [
                        {
                          "distance": 1000.0,
                          "duration": 120.0,
                          "geometry": "testGeometry"
                        }
                      ]
                    }
                    """));

        org.springframework.test.util.ReflectionTestUtils.setField(
                routingProvider, "responseTimeout", 5000
        );

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(coordinates)
        );
    }


    @Test
    void calculateRoute_shouldThrowWhenConnectionCannotBeEstablished() {

        // Use a local port that has no server listening.
        org.springframework.test.util.ReflectionTestUtils.setField(
                routingProvider,
                "baseUrl",
                "http://127.0.0.1:1"
        );

        List<Coordinate> coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(coordinates)
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDistanceIsZero() {
        enqueueOsrmResponse(0.0, 900.0, "validGeometry");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDistanceIsNegative() {
        enqueueOsrmResponse(-100.0, 900.0, "validGeometry");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDurationIsZero() {
        enqueueOsrmResponse(12000.0, 0.0, "validGeometry");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDurationIsNegative() {
        enqueueOsrmResponse(12000.0, -100.0, "validGeometry");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDistanceIsNaN() {
        enqueueOsrmResponse("NaN", "900.0", "\"validGeometry\"");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenDurationIsInfinite() {
        enqueueOsrmResponse("12000.0", "Infinity", "\"validGeometry\"");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenGeometryIsBlank() {
        enqueueOsrmResponse(12000.0, 900.0, "");

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }

    @Test
    void calculateRoute_shouldThrowExceptionWhenGeometryIsNull() {
        enqueueOsrmResponse(12000.0, 900.0, null);

        assertThrows(
                RoutingException.class,
                () -> routingProvider.calculateRoute(validCoordinates())
        );
    }


    private List<Coordinate> validCoordinates() {
        return List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );
    }

    /**
     * Queues an OSRM response with configurable route values.
     * String parameters allow testing JSON values such as NaN and Infinity.
     */
    private void enqueueOsrmResponse(
            String distance,
            String duration,
            String geometryJson
    ) {
        String geometry = geometryJson == null
                ? "null"
                : geometryJson.startsWith("\"")
                ? geometryJson
                : "\"" + geometryJson + "\"";

        String response = """
            {
              "code": "Ok",
              "routes": [
                {
                  "distance": %s,
                  "duration": %s,
                  "geometry": %s
                }
              ]
            }
            """.formatted(distance, duration, geometry);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(response)
        );
    }

    private void enqueueOsrmResponse(
            double distance,
            double duration,
            String geometry
    ) {
        enqueueOsrmResponse(
                Double.toString(distance),
                Double.toString(duration),
                geometry
        );
    }




}

