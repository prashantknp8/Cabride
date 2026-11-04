
package com.cabride.ride_service.Dto.Response;

import com.cabride.ride_service.Entity.RideStop;
import lombok.Getter;

import java.util.List;

@Getter
public class RouteInsertionResult {

    private final boolean feasible;
    private final String reason;

    private final double additionalDistanceMeters;
    private final double additionalDurationSeconds;

    private final List<RideStop> proposedStops;

    private final int pickupIndex;
    private final int dropIndex;

    // Estimated travel time until the new passenger's pickup.
    private final double pickupWaitSeconds;

    // Individual ETA impact for every existing passenger.
    private final List<PassengerDetourResult> passengerImpacts;

    /**
     * Constructor without passenger impact details.
     */
    public RouteInsertionResult(
            boolean feasible,
            String reason,
            double additionalDistanceMeters,
            double additionalDurationSeconds,
            List<RideStop> proposedStops,
            int pickupIndex,
            int dropIndex
    ) {
        this(
                feasible,
                reason,
                additionalDistanceMeters,
                additionalDurationSeconds,
                proposedStops,
                pickupIndex,
                dropIndex,
                0.0,
                List.of()
        );
    }

    /**
     * Constructor with pickup waiting time.
     */
    public RouteInsertionResult(
            boolean feasible,
            String reason,
            double additionalDistanceMeters,
            double additionalDurationSeconds,
            List<RideStop> proposedStops,
            int pickupIndex,
            int dropIndex,
            double pickupWaitSeconds
    ) {
        this(
                feasible,
                reason,
                additionalDistanceMeters,
                additionalDurationSeconds,
                proposedStops,
                pickupIndex,
                dropIndex,
                pickupWaitSeconds,
                List.of()
        );
    }

    /**
     * Complete constructor containing all route evaluation results.
     */
    public RouteInsertionResult(
            boolean feasible,
            String reason,
            double additionalDistanceMeters,
            double additionalDurationSeconds,
            List<RideStop> proposedStops,
            int pickupIndex,
            int dropIndex,
            double pickupWaitSeconds,
            List<PassengerDetourResult> passengerImpacts
    ) {
        this.feasible = feasible;
        this.reason = reason;
        this.additionalDistanceMeters = additionalDistanceMeters;
        this.additionalDurationSeconds = additionalDurationSeconds;
        this.proposedStops = proposedStops == null
                ? List.of() : List.copyOf(proposedStops);
        this.pickupIndex = pickupIndex;
        this.dropIndex = dropIndex;
        this.pickupWaitSeconds = pickupWaitSeconds;
        this.passengerImpacts = passengerImpacts == null
                ? List.of() : List.copyOf(passengerImpacts);
    }
}
