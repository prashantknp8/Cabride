package com.cabride.ride_service.Client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.RouteMatcher;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRouteResponse {
    private String code;

    private List<Route> routes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Route{
        private double distance;

        private double duration;

        private String geometry;
        private List<Leg> legs;
    }


    /**
     * Represents the OSRM travel information between two consecutive
     * coordinates in the requested route.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {

        private double distance;
        private double duration;
    }

}
