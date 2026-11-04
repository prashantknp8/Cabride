
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.StopType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteOrderValidatorTest {

    private RideStop createStop(UUID bookingId, StopType stopType) {
        RideStop stop = new RideStop();
        stop.setBookingId(bookingId);
        stop.setStopType(stopType);
        return stop;
    }

    @Test
    void isPickupBeforeDrop_shouldReturnTrueForValidSequence() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.PICKUP),
                createStop(bookingId, StopType.DROP)
        );

        assertTrue(RouteOrderValidator.isPickupBeforeDrop(stops));
    }

    @Test
    void isPickupBeforeDrop_shouldReturnTrueForMultipleBookings() {
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingA, StopType.PICKUP),
                createStop(bookingB, StopType.PICKUP),
                createStop(bookingA, StopType.DROP),
                createStop(bookingB, StopType.DROP)
        );

        assertTrue(RouteOrderValidator.isPickupBeforeDrop(stops));
    }

    @Test
    void isPickupBeforeDrop_shouldReturnFalseWhenDropComesFirst() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.DROP),
                createStop(bookingId, StopType.PICKUP)
        );

        assertFalse(RouteOrderValidator.isPickupBeforeDrop(stops));
    }

    @Test
    void isPickupBeforeDrop_shouldRejectDuplicatePickup() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.PICKUP),
                createStop(bookingId, StopType.PICKUP),
                createStop(bookingId, StopType.DROP)
        );

        assertFalse(RouteOrderValidator.isPickupBeforeDrop(stops));
    }

    @Test
    void isPickupBeforeDrop_shouldRejectMissingDrop() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.PICKUP)
        );

        assertFalse(RouteOrderValidator.isPickupBeforeDrop(stops));
    }

    @Test
    void isPickupBeforeDrop_shouldRejectEmptyOrNullList() {
        assertFalse(RouteOrderValidator.isPickupBeforeDrop(List.of()));
        assertFalse(RouteOrderValidator.isPickupBeforeDrop(null));
    }
}
