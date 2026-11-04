package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Enums.StopStatus;

import java.util.ArrayList;
import java.util.List;

public final class RouteInsertionUtils {

    private RouteInsertionUtils() {

    }

    public static List<int[]> generateInsertionPositions(int existingStopCount) {

        if (existingStopCount < 0) {
            throw new IllegalArgumentException(
                    "Existing stop count cannot be negative"
            );
        }

        List<int[]> positions = new ArrayList<>();

        // The final route contains two additional stops.
        int finalStopCount = existingStopCount + 2;

        // Pickup must appear before drop-off.
        for (int pickupIndex = 0; pickupIndex < finalStopCount - 1; pickupIndex++) {

            for (int dropIndex = pickupIndex + 1;
                 dropIndex < finalStopCount;
                 dropIndex++) {

                positions.add(new int[]{pickupIndex, dropIndex});
            }
        }

        return positions;
    }


    /**
     * Returns insertion positions for the pending portion of a route.
     * <p>
     * Completed stops are excluded from insertion positions.
     * Existing pending stops retain their relative order.
     *
     * @param completedStopCount number of completed stops
     * @param pendingStopCount   number of pending stops
     * @return valid pickup/drop-off insertion positions in the final route
     */
    public static List<int[]> generatePendingInsertionPositions(
            int completedStopCount,
            int pendingStopCount
    ) {
        if (completedStopCount < 0 || pendingStopCount < 0) {
            throw new IllegalArgumentException(
                    "Stop counts cannot be negative"
            );
        }

        // Only pending stops and the two new stops participate in insertion.
        List<int[]> pendingPositions =
                generateInsertionPositions(pendingStopCount);

        List<int[]> finalPositions = new ArrayList<>();

        // Offset indexes to account for the completed stops at the beginning.
        for (int[] position : pendingPositions) {
            finalPositions.add(new int[]{
                    position[0] + completedStopCount,
                    position[1] + completedStopCount
            });
        }

        return finalPositions;
    }


    /**
     * Generates pickup/drop-off insertion positions within each consecutive
     * segment of PENDING stops.
     * <p>
     * Fixed stops divide the route into independent insertion segments.
     * Each returned pair represents final-route indexes for one candidate.
     */
    public static List<int[]> generateSegmentInsertionPositions(
            List<StopStatus> stopStatuses
    ) {
        if (stopStatuses == null) {
            throw new IllegalArgumentException(
                    "Stop statuses cannot be null"
            );
        }

        List<int[]> result = new ArrayList<>();

        int segmentStart = 0;
        int pendingCount = 0;

        // Scan the existing route and process each consecutive pending segment.
        for (int i = 0; i <= stopStatuses.size(); i++) {

            boolean segmentEnded = i == stopStatuses.size()
                    || stopStatuses.get(i) != StopStatus.PENDING;

            if (segmentEnded) {

                // Empty segments cannot accept a new pickup/drop-off pair.
                if (pendingCount > 0) {

                    List<int[]> localPositions =
                            generateInsertionPositions(pendingCount);

                    // Convert segment-local indexes into route indexes.
                    for (int[] position : localPositions) {
                        result.add(new int[]{
                                segmentStart + position[0],
                                segmentStart + position[1]
                        });
                    }
                }

                // The next pending segment starts after this fixed stop.
                if (i < stopStatuses.size()) {
                    segmentStart = i + 1;
                }

                pendingCount = 0;

            } else {
                pendingCount++;
            }
        }

        return result;
    }


    /**
     * Determines whether a stop is fixed and must not be reordered.
     */
    public static boolean isFixedStop(StopStatus status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Stop status cannot be null"
            );
        }

        return status == StopStatus.ARRIVED
                || status == StopStatus.COMPLETED
                || status == StopStatus.SKIPPED;
    }


    /**
     * Generates insertion positions across all pending segments.
     *
     * Fixed stops remain in their original positions relative to one another.
     * New pickup and drop-off stops can be inserted into different pending
     * segments, provided pickup occurs before drop-off.
     *
     * Returned indexes refer to the final route after both insertions.
     *
     * @param stopStatuses existing stop statuses in route order
     * @return all pickup/drop-off insertion index pairs
     */

    /**
     * Generates every possible pickup/drop-off position pair.
     *
     * Indexes refer to the final route after both new stops are inserted.
     * Therefore, pickup must always appear before drop-off.
     */
    public static List<int[]> generateCrossSegmentInsertionPositions(
            int existingStopCount
    ) {
        if (existingStopCount < 0) {
            throw new IllegalArgumentException(
                    "Existing stop count cannot be negative"
            );
        }

        List<int[]> positions = new ArrayList<>();

        int finalStopCount = existingStopCount + 2;

        for (int pickupIndex = 0;
             pickupIndex < finalStopCount - 1;
             pickupIndex++) {

            for (int dropIndex = pickupIndex + 1;
                 dropIndex < finalStopCount;
                 dropIndex++) {

                positions.add(new int[]{pickupIndex, dropIndex});
            }
        }

        return positions;
    }

    /**
     * Checks whether both inserted stops occur after every fixed stop.
     *
     * Existing fixed stops are never moved or reordered.
     */
    public static boolean areInsertionsAfterFixedStops(
            List<StopStatus> stopStatuses,
            int pickupIndex,
            int dropIndex
    ) {
        if (stopStatuses == null) {
            throw new IllegalArgumentException(
                    "Stop statuses cannot be null"
            );
        }

        if (pickupIndex < 0 || dropIndex <= pickupIndex) {
            return false;
        }

        int lastFixedIndex = -1;

        for (int i = 0; i < stopStatuses.size(); i++) {
            StopStatus status = stopStatuses.get(i);

            if (status == null) {
                throw new IllegalArgumentException(
                        "Stop status cannot be null"
                );
            }

            if (isFixedStop(status)) {
                lastFixedIndex = i;
            }
        }

        return pickupIndex > lastFixedIndex
                && dropIndex > lastFixedIndex;
    }
}