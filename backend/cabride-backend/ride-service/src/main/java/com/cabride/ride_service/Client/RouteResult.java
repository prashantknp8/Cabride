
package com.cabride.ride_service.Client;

import lombok.Getter;

import java.util.List;

@Getter
public class RouteResult {

    private final double distanceMeters;
    private final double durationSeconds;
    private final String geometry;

    // Individual road segments returned by OSRM.
    private final List<RouteLegResult> legs;

    public RouteResult(
            double distanceMeters,
            double durationSeconds,
            String geometry
    ) {
        this(distanceMeters, durationSeconds, geometry, List.of());
    }

    public RouteResult(
            double distanceMeters,
            double durationSeconds,
            String geometry,
            List<RouteLegResult> legs
    ) {
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.geometry = geometry;
        this.legs = legs == null ? List.of() : List.copyOf(legs);
    }

    public double getDistanceKilometers() {
        return distanceMeters / 1000.0;
    }

    public double getDurationMinutes() {
        return durationSeconds / 60.0;
    }
}
