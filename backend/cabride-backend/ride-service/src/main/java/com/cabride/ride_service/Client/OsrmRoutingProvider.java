package com.cabride.ride_service.Client;

import com.cabride.ride_service.Exception.RoutingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OsrmRoutingProvider implements RoutingProvider{

    private final WebClient.Builder webClientBuilder;

    @Value("${cabride.routing.osrm.base-url}")
    private String baseUrl;

    @Value("${cabride.routing.osrm.connect-timeout}")
    private int connectTimeout;

    @Value("${cabride.routing.osrm.response-timeout}")
    private int responseTimeout;

    @Override
    public RouteResult calculateRoute(List<Coordinate> coordinates) {
        if(coordinates==null || coordinates.size()<2){
            throw new IllegalArgumentException(
                    "At least two coordinates are required"
            );
        }

        // OSRM expects every coordinate in longitude,latitude order.
        String coordinatesPath = coordinates.stream()
                .map(coordinate ->
                        coordinate.getLongitude() + "," + coordinate.getLatitude())
                .collect(Collectors.joining(";"));

        // Configure connection and response timeouts for OSRM requests.
        HttpClient httpClient = HttpClient.create()
                .option(
                        io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        connectTimeout
                )
                .responseTimeout(Duration.ofMillis(responseTimeout));

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        try {
            OsrmRouteResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/{coordinates}")
                            .queryParam("overview", "full")
                            .queryParam("geometries", "polyline")
                            .build(coordinatesPath))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse -> clientResponse.createException()
                    )
                    .bodyToMono(OsrmRouteResponse.class)
                    .block();

            // Validate the response returned by OSRM.
            if (response == null
                    || !"Ok".equals(response.getCode())
                    || response.getRoutes() == null
                    || response.getRoutes().isEmpty()) {

                throw new IllegalStateException(
                        "OSRM could not calculate the requested route"
                );
            }

            OsrmRouteResponse.Route route = response.getRoutes().get(0);

// Reject a missing route object before accessing its properties.
            if (route == null) {
                throw new RoutingException("OSRM returned an empty route");
            }

// Distance must be finite and positive.
            if (!Double.isFinite(route.getDistance()) || route.getDistance() <= 0) {
                throw new RoutingException("OSRM returned an invalid route distance");
            }

// Duration must be finite and positive.
            if (!Double.isFinite(route.getDuration()) || route.getDuration() <= 0) {
                throw new RoutingException("OSRM returned an invalid route duration");
            }

// Geometry is required for later route visualization and processing.
            if (route.getGeometry() == null || route.getGeometry().isBlank()) {
                throw new RoutingException("OSRM returned empty route geometry");
            }


// Convert OSRM's individual route legs into our internal DTO.
            List<RouteLegResult> legs = route.getLegs().stream()
                    .map(leg -> new RouteLegResult(
                            leg.getDistance(),
                            leg.getDuration()
                    ))
                    .toList();

// Return both overall route metrics and individual leg metrics.
            return new RouteResult(
                    route.getDistance(),
                    route.getDuration(),
                    route.getGeometry(),
                    legs
            );

        } catch (WebClientResponseException exception) {

            // Preserve HTTP status information in the routing error.
            throw new RoutingException(
                    "OSRM routing request failed with HTTP status: "
                            + exception.getStatusCode(),
                    exception
            );

        } catch (RuntimeException exception) {

            // Preserve already-normalized routing failures.
            if (exception instanceof RoutingException) {
                throw exception;
            }

            // Normalize connection failures, timeouts, and other runtime errors.
            throw new RoutingException(
                    "Unable to calculate route using OSRM",
                    exception
            );
        }
    }
}