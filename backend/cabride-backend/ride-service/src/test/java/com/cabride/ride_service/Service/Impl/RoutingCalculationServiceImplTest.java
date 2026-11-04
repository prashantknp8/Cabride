
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.Coordinate;
import com.cabride.ride_service.Client.RouteResult;
import com.cabride.ride_service.Client.RoutingProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceImplTest {

    @Mock
    private RoutingProvider routingProvider;

    @InjectMocks
    private RouteCalculationServiceImpl routeCalculationService;

    private List<Coordinate> coordinates;

    @BeforeEach
    void setUp() {
        coordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.2329, 26.5123)
        );
    }

    @Test
    void calculateRoute_shouldReturnRouteResult() {

        RouteResult expectedResult = new RouteResult(
                12500.5,
                900.0,
                "encodedPolyline"
        );

        when(routingProvider.calculateRoute(coordinates))
                .thenReturn(expectedResult);

        RouteResult actualResult =
                routeCalculationService.calculateRoute(coordinates);

        assertNotNull(actualResult);
        assertEquals(12500.5, actualResult.getDistanceMeters());
        assertEquals(900.0, actualResult.getDurationSeconds());
        assertEquals("encodedPolyline", actualResult.getGeometry());

        verify(routingProvider, times(1))
                .calculateRoute(coordinates);
    }

    @Test
    void calculateRoute_shouldPropagateRoutingException() {

        when(routingProvider.calculateRoute(coordinates))
                .thenThrow(new RuntimeException("Routing service unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> routeCalculationService.calculateRoute(coordinates)
        );

        verify(routingProvider, times(1))
                .calculateRoute(coordinates);
    }

    @Test
    void calculateRoute_shouldPassAllCoordinatesToProvider() {

        List<Coordinate> multipleCoordinates = List.of(
                new Coordinate(80.3319, 26.4499),
                new Coordinate(80.3000, 26.4700),
                new Coordinate(80.2329, 26.5123)
        );

        RouteResult expectedResult = new RouteResult(
                15000.0,
                1200.0,
                "multiStopPolyline"
        );

        when(routingProvider.calculateRoute(multipleCoordinates))
                .thenReturn(expectedResult);

        RouteResult result =
                routeCalculationService.calculateRoute(multipleCoordinates);

        assertEquals(15000.0, result.getDistanceMeters());

        verify(routingProvider)
                .calculateRoute(multipleCoordinates);
    }
}
