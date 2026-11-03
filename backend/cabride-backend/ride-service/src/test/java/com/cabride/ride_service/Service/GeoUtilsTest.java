package com.cabride.ride_service.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void calculateDistance_shouldReturnZeroForSameCoordinates() {
        double distance = GeoUtils.calculateDistance(
                26.4499, 80.3319,
                26.4499, 80.3319
        );

        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void calculateDistance_shouldCalculateDistanceBetweenTwoLocations() {
        // Kanpur Central to IIT Kanpur (approximate coordinates).
        double distance = GeoUtils.calculateDistance(
                26.4499, 80.3319,
                26.5123, 80.2329
        );

        assertTrue(distance > 0);
        assertTrue(distance < 20);
    }

    @Test
    void calculateDistance_shouldReturnSameDistanceInReverseDirection() {
        double distance1 = GeoUtils.calculateDistance(
                26.4499, 80.3319,
                26.5123, 80.2329
        );

        double distance2 = GeoUtils.calculateDistance(
                26.5123, 80.2329,
                26.4499, 80.3319
        );

        assertEquals(distance1, distance2, 0.001);
    }
    @Test
    void calculateBearing_shouldReturnEastwardBearing() {
        // Moving east along the equator should produce approximately 90°.
        double bearing = GeoUtils.calculateBearing(0.0, 0.0, 0.0, 1.0);

        assertEquals(90.0, bearing, 0.1);
    }

    @Test
    void calculateBearing_shouldReturnNorthwardBearing() {
        // Moving north along the same longitude should produce approximately 0°.
        double bearing = GeoUtils.calculateBearing(0.0, 0.0, 1.0, 0.0);

        assertEquals(0.0, bearing, 0.1);
    }

    @Test
    void calculateBearing_shouldReturnSouthwardBearing() {
        double bearing = GeoUtils.calculateBearing(1.0, 0.0, 0.0, 0.0);

        assertEquals(180.0, bearing, 0.1);
    }

    @Test
    void calculateBearing_shouldReturnWestwardBearing() {
        double bearing = GeoUtils.calculateBearing(0.0, 1.0, 0.0, 0.0);

        assertEquals(270.0, bearing, 0.1);
    }

    @Test
    void calculateBearingDifference_shouldCalculateNormalDifference() {
        assertEquals(
                10.0,
                GeoUtils.calculateBearingDifference(90.0, 100.0),
                0.001
        );
    }

    @Test
    void calculateBearingDifference_shouldHandleOppositeDirections() {
        assertEquals(
                180.0,
                GeoUtils.calculateBearingDifference(90.0, 270.0),
                0.001
        );
    }

    @Test
    void calculateBearingDifference_shouldHandleCircularAngles() {
        // 350° and 10° differ by only 20°.
        assertEquals(
                20.0,
                GeoUtils.calculateBearingDifference(350.0, 10.0),
                0.001
        );
    }

    @Test
    void calculateBearingDifference_shouldHandleNegativeBearings() {
        assertEquals(
                20.0,
                GeoUtils.calculateBearingDifference(-10.0, 10.0),
                0.001
        );
    }

    @Test
    void calculateBearingDifference_shouldReturnZeroForSameBearings() {
        assertEquals(
                0.0,
                GeoUtils.calculateBearingDifference(90.0, 90.0),
                0.001
        );
    }
}