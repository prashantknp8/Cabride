package com.cabride.ride_service.Client;

import java.util.List;

public interface RoutingProvider {

    RouteResult calculateRoute(List<Coordinate> coordinates);
}
