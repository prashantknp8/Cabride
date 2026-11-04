package com.cabride.ride_service.Service.Impl;


import com.cabride.ride_service.Enums.StopStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cabride.ride_service.Service.Impl.RouteInsertionUtils.generateInsertionPositions;
import static org.junit.jupiter.api.Assertions.*;

class RouteInsertionUtilsTest {

    @Test
    void generateInsertionPositions_shouldReturnSixPositionsForTwoStops() {
        List<int[]> positions =
                generateInsertionPositions(2);

        assertEquals(6, positions.size());
    }

    @Test
    void generateInsertionPositions_shouldPreservePickupBeforeDropOrder() {
        List<int[]> positions =
                generateInsertionPositions(3);

        for (int[] position : positions) {
            assertTrue(position[0] < position[1]);
        }
    }

    @Test
    void generateInsertionPositions_shouldReturnOnePositionForZeroStops() {
        List<int[]> positions =
                generateInsertionPositions(0);

        assertEquals(1, positions.size());
        assertArrayEquals(new int[]{0, 1}, positions.get(0));
    }

    @Test
    void generateInsertionPositions_shouldReturnCorrectPositionsForOneStop() {
        List<int[]> positions =
                generateInsertionPositions(1);

        assertEquals(3, positions.size());

        assertArrayEquals(new int[]{0, 1}, positions.get(0));
        assertArrayEquals(new int[]{0, 2}, positions.get(1));
        assertArrayEquals(new int[]{1, 2}, positions.get(2));
    }

    @Test
    void generateInsertionPositions_shouldRejectNegativeStopCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> generateInsertionPositions(-1)
        );
    }


    @Test
    void generatePendingInsertionPositions_shouldExcludeCompletedStops() {
        List<int[]> positions =
                RouteInsertionUtils.generatePendingInsertionPositions(2, 2);

        assertEquals(6, positions.size());

        for (int[] position : positions) {
            assertTrue(position[0] >= 2);
            assertTrue(position[1] >= 2);
            assertTrue(position[0] < position[1]);
        }
    }

    @Test
    void generatePendingInsertionPositions_shouldHandleAllStopsCompleted() {
        List<int[]> positions =
                RouteInsertionUtils.generatePendingInsertionPositions(4, 0);

        assertEquals(1, positions.size());
        assertArrayEquals(new int[]{4, 5}, positions.get(0));
    }

    @Test
    void generatePendingInsertionPositions_shouldRejectNegativeCounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RouteInsertionUtils.generatePendingInsertionPositions(-1, 2)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RouteInsertionUtils.generatePendingInsertionPositions(2, -1)
        );
    }

    @Test
    void generateSegmentInsertionPositions_shouldHandleInterleavedFixedStops() {
        List<StopStatus> statuses = List.of(
                StopStatus.COMPLETED,
                StopStatus.PENDING,
                StopStatus.ARRIVED,
                StopStatus.PENDING,
                StopStatus.PENDING
        );

        List<int[]> positions =
                RouteInsertionUtils.generateSegmentInsertionPositions(statuses);

        // One pair in the first pending segment and six in the second.
        assertEquals(9, positions.size());

        for (int[] position : positions) {
            assertTrue(position[0] < position[1]);
        }
    }

    @Test
    void generateSegmentInsertionPositions_shouldReturnEmptyWhenNoPendingStopsExist() {
        List<StopStatus> statuses = List.of(
                StopStatus.COMPLETED,
                StopStatus.ARRIVED,
                StopStatus.SKIPPED
        );

        assertTrue(
                RouteInsertionUtils.generateSegmentInsertionPositions(statuses).isEmpty()
        );
    }

    @Test
    void generateSegmentInsertionPositions_shouldRejectNullStatuses() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RouteInsertionUtils.generateSegmentInsertionPositions(null)
        );
    }

    @Test
    void generateSegmentInsertionPositions_shouldKeepCandidatesInsidePendingSegments() {

        List<StopStatus> statuses = List.of(
                StopStatus.COMPLETED,
                StopStatus.PENDING,
                StopStatus.ARRIVED,
                StopStatus.PENDING,
                StopStatus.PENDING
        );

        List<int[]> positions =
                RouteInsertionUtils.generateSegmentInsertionPositions(statuses);

        assertEquals(9, positions.size());

        // First segment starts at index 1.
        assertArrayEquals(new int[]{1, 2}, positions.get(0));

        // Second segment starts at index 3.
        assertArrayEquals(new int[]{3, 4}, positions.get(3));

        for (int[] position : positions) {
            assertTrue(position[0] < position[1]);
        }
    }

    @Test
    void isFixedStop_shouldReturnTrueForCompletedStop() {
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.COMPLETED));
    }

    @Test
    void isFixedStop_shouldReturnTrueForArrivedStop() {
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.ARRIVED));
    }

    @Test
    void isFixedStop_shouldReturnTrueForSkippedStop() {
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.SKIPPED));
    }

    @Test
    void isFixedStop_shouldReturnFalseForPendingStop() {
        assertFalse(RouteInsertionUtils.isFixedStop(StopStatus.PENDING));
    }

    @Test
    void isFixedStop_shouldRejectNullStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RouteInsertionUtils.isFixedStop(null)
        );
    }

    @Test
    void isFixedStop_shouldClassifyAllStatusesCorrectly() {
        assertFalse(RouteInsertionUtils.isFixedStop(StopStatus.PENDING));
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.ARRIVED));
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.COMPLETED));
        assertTrue(RouteInsertionUtils.isFixedStop(StopStatus.SKIPPED));
    }




    @Test
    void generateCrossSegmentInsertionPositions_shouldGenerateAllOrderedPairs() {
        List<int[]> positions =
                RouteInsertionUtils.generateCrossSegmentInsertionPositions(4);

        assertEquals(15, positions.size());

        assertArrayEquals(new int[]{0, 1}, positions.get(0));
        assertArrayEquals(new int[]{0, 5}, positions.get(4));
        assertArrayEquals(new int[]{4, 5}, positions.get(14));
    }


    @Test
    void areInsertionsAfterFixedStops_shouldAllowInsertionsAfterLastFixedStop() {
        List<StopStatus> statuses = List.of(
                StopStatus.COMPLETED,
                StopStatus.ARRIVED,
                StopStatus.PENDING,
                StopStatus.PENDING
        );

        assertTrue(
                RouteInsertionUtils.areInsertionsAfterFixedStops(
                        statuses, 2, 5
                )
        );
    }

    @Test
    void areInsertionsAfterFixedStops_shouldRejectInsertionBeforeLastFixedStop() {
        List<StopStatus> statuses = List.of(
                StopStatus.COMPLETED,
                StopStatus.ARRIVED,
                StopStatus.PENDING
        );

        assertFalse(
                RouteInsertionUtils.areInsertionsAfterFixedStops(
                        statuses, 1, 4
                )
        );
    }

    @Test
    void areInsertionsAfterFixedStops_shouldRejectInvalidIndexes() {
        List<StopStatus> statuses = List.of(StopStatus.PENDING);

        assertFalse(
                RouteInsertionUtils.areInsertionsAfterFixedStops(
                        statuses, 2, 1
                )
        );
    }


}
