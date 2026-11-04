
package com.cabride.ride_service.Dto.Response;

import com.cabride.ride_service.Entity.RideStop;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents one possible route after inserting a passenger's
 * pickup and drop-off stops.
 *
 */



@Getter
@AllArgsConstructor
public class RouteInsertionCandidate {

    private final List<RideStop> proposedStops;

    private final int pickupIndex;

    private final int dropIndex;
}
