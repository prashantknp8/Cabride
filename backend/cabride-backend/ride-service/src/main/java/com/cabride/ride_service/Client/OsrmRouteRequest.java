package com.cabride.ride_service.Client;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OsrmRouteRequest {
    private final List<Coordinate> coordinates;
}
