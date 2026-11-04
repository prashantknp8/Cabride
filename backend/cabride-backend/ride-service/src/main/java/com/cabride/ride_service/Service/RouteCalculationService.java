package com.cabride.ride_service.Service;

import com.cabride.ride_service.Client.Coordinate;
import com.cabride.ride_service.Client.RouteResult;

import java.util.List;

public interface RouteCalculationService {
    RouteResult calculateRoute(List<Coordinate> coordinates);
}
