
package com.cabride.ride_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PassengerDetourResult {

    private final UUID bookingId;

    private final double originalPickupEtaSeconds;
    private final double proposedPickupEtaSeconds;
    private final double pickupDelaySeconds;

    private final double originalDropEtaSeconds;
    private final double proposedDropEtaSeconds;
    private final double dropDelaySeconds;

    private final double maximumDelaySeconds;
}
