
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.StopType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates pickup and drop-off ordering for every booking in a route.
 */
public final class RouteOrderValidator {

    private RouteOrderValidator() {
        // Utility class; prevent object creation.
    }


    public static boolean isPickupBeforeDrop(List<RideStop> orderedStops) {

        if (orderedStops == null || orderedStops.isEmpty()) {
            return false;
        }

        // Store the position of each booking's pickup and drop-off.
        Map<UUID, Integer> pickupPositions = new HashMap<>();
        Map<UUID, Integer> dropPositions = new HashMap<>();

        for (int index = 0; index < orderedStops.size(); index++) {

            RideStop stop = orderedStops.get(index);

            if (stop == null || stop.getBookingId() == null
                    || stop.getStopType() == null) {
                return false;
            }

            UUID bookingId = stop.getBookingId();

            if (stop.getStopType() == StopType.PICKUP) {

                // Reject duplicate pickup stops for the same booking.
                if (pickupPositions.putIfAbsent(bookingId, index) != null) {
                    return false;
                }

            } else if (stop.getStopType() == StopType.DROP) {

                // Reject duplicate drop-off stops for the same booking.
                if (dropPositions.putIfAbsent(bookingId, index) != null) {
                    return false;
                }
            }
        }

        // Every booking must have exactly one pickup and one drop-off.
        if (!pickupPositions.keySet().equals(dropPositions.keySet())) {
            return false;
        }

        // Every pickup must appear before its corresponding drop-off.
        for (UUID bookingId : pickupPositions.keySet()) {
            if (pickupPositions.get(bookingId) >= dropPositions.get(bookingId)) {
                return false;
            }
        }

        return true;
    }
}
