
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.Coordinate;
import com.cabride.ride_service.Client.RouteResult;
import com.cabride.ride_service.Client.RoutingProvider;
import com.cabride.ride_service.Service.RouteCalculationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteCalculationServiceImpl implements RouteCalculationService {

    private final RoutingProvider routingProvider;

    @Override
    public RouteResult calculateRoute(List<Coordinate> coordinates) {

        // Delegate route calculation to the configured provider.
        return routingProvider.calculateRoute(coordinates);
    }
}
