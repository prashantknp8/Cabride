
package com.cabride.ride_service.Client;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Distance and duration for one road segment between two consecutive stops.
 */
@Getter
@AllArgsConstructor
public class RouteLegResult {

    private final double distanceMeters;
    private final double durationSeconds;
}
