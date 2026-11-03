package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Exception.ResourceNotFoundException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import com.cabride.ride_service.Service.RideStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideStopServiceImpl implements RideStopService {

    private final RideStopRepository rideStopRepository;
    private final RideRepository rideRepository;
    private final RideBookingRepository rideBookingRepository;


    @Override
    @Transactional
    public void createStops(UUID rideId, UUID bookingId, Double pickupLat, Double pickupLon, Double dropLat, Double dropLon) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ride not found"));

        RideBooking booking = rideBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride Booking not found"));

        if (!booking.getRideId().equals(ride.getId())) {
            throw new IllegalArgumentException(
                    "Booking does not belong to this ride"
            );
        }
        if (pickupLat == null || pickupLon == null
                || dropLat == null || dropLon == null
                || pickupLat < -90 || pickupLat > 90
                || dropLat < -90 || dropLat > 90
                || pickupLon < -180 || pickupLon > 180
                || dropLon < -180 || dropLon > 180) {
            throw new IllegalArgumentException(
                    "Invalid pickup or drop-off coordinates"
            );
        }
        if (!rideStopRepository.findByBookingId(bookingId).isEmpty()) {
            throw new IllegalStateException(
                    "Stops already exist for this booking"
            );
        }

        List<RideStop> existingStops = rideStopRepository.findByRideIdOrderBySequenceAsc(rideId);

        //Append new stops after the current route instead of reusing sequence numbers.
        int nextSequence = existingStops.isEmpty() ? 1 : existingStops.get(existingStops.size() - 1).getSequence() + 1;

        RideStop pickup = RideStop.builder()
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .latitude(pickupLat)
                .longitude(pickupLon)
                .sequence(nextSequence)
                .status(StopStatus.PENDING)
                .build();
        RideStop drop = RideStop.builder()
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .latitude(dropLat)
                .longitude(dropLon)
                .sequence(nextSequence + 1)
                .status(StopStatus.PENDING)
                .build();

        rideStopRepository.saveAll(List.of(pickup, drop));


    }

    @Override
    @Transactional(readOnly = true)
    public List<RideStop> getRideStops(UUID rideId) {
        if (!rideRepository.existsById(rideId)) {
            throw new ResourceNotFoundException("Ride not found");
        }
        return rideStopRepository.findByRideIdOrderBySequenceAsc(rideId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideStop> getBookingStops(UUID bookingId) {
        if (!rideBookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Ride booking not found");
        }

        return rideStopRepository.findByBookingId(bookingId);
    }

    @Override
    @Transactional
    public RideStop updateStopStatus(UUID stopId, StopStatus status) {
        RideStop existingStop = rideStopRepository.findById(stopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ride Stop not found"));

        rideRepository.findByIdForUpdate(existingStop.getRideId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ride not found"));

        RideStop stop = rideStopRepository.findById(stopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ride Stop not found"));

        if (status == null) {
            throw new IllegalArgumentException("Stop status is required");

        }

        boolean validTransition =
                (stop.getStatus() == StopStatus.PENDING
                        && (status == StopStatus.ARRIVED
                        || status == StopStatus.SKIPPED))
                        ||
                        (stop.getStatus() == StopStatus.ARRIVED
                                && (status == StopStatus.COMPLETED
                                || status == StopStatus.SKIPPED));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid stop status transition: "
                            + stop.getStatus() + " -> " + status
            );
        }
        stop.setStatus(status);
        if (status == StopStatus.COMPLETED) {
            stop.setCompletedAt(LocalDateTime.now());

            if (stop.getStopType() == StopType.DROP) {

                RideBooking booking = rideBookingRepository
                        .findById(stop.getBookingId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Booking not found"));

                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                    throw new InvalidRideStatusException(
                            "Only confirmed bookings can be completed"
                    );
                }

                booking.setStatus(BookingStatus.COMPLETED);
                rideBookingRepository.save(booking);
            }
        }

        return rideStopRepository.save(stop);
    }


    @Override
    @Transactional
    public void reorderStops(UUID rideId, List<UUID> orderedStopIds) {

        rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ride not found"));

        if (orderedStopIds == null) {
            throw new IllegalArgumentException("Stop order is required");
        }

        List<RideStop> stops =
                rideStopRepository.findByRideIdOrderBySequenceAsc(rideId);

        List<RideStop> pendingStops = stops.stream()
                .filter(stop -> stop.getStatus() == StopStatus.PENDING)
                .toList();

        Set<UUID> requestedIds = new HashSet<>(orderedStopIds);

        Set<UUID> pendingIds = pendingStops.stream()
                .map(RideStop::getId)
                .collect(Collectors.toSet());

        if (requestedIds.size() != orderedStopIds.size()
                || !requestedIds.equals(pendingIds)) {
            throw new IllegalArgumentException(
                    "Provide every pending stop exactly once"
            );
        }

        if (orderedStopIds.isEmpty()) {
            return;
        }

        Map<UUID, RideStop> stopMap = stops.stream()
                .collect(Collectors.toMap(RideStop::getId, stop -> stop));

        // Every non-pending stop is a fixed route commitment.
        Map<UUID, Integer> originalSegments = new HashMap<>();

        int segment = 0;

        for (RideStop stop : stops) {
            if (stop.getStatus() == StopStatus.PENDING) {
                originalSegments.put(stop.getId(), segment);
            } else {
                segment++;
            }
        }

        // Group the requested order by each stop's original segment.
        Map<Integer, List<UUID>> requestedSegments = new LinkedHashMap<>();

        for (UUID stopId : orderedStopIds) {
            Integer segmentId = originalSegments.get(stopId);

            requestedSegments
                    .computeIfAbsent(segmentId, key -> new ArrayList<>())
                    .add(stopId);
        }

        // Verify that no pending stop crosses a fixed stop.
        Map<Integer, Set<UUID>> originalSegmentIds = new HashMap<>();

        for (RideStop stop : pendingStops) {
            Integer segmentId = originalSegments.get(stop.getId());

            originalSegmentIds
                    .computeIfAbsent(segmentId, key -> new HashSet<>())
                    .add(stop.getId());
        }

        if (!requestedSegments.keySet().equals(originalSegmentIds.keySet())) {
            throw new IllegalArgumentException(
                    "Pending stops cannot cross fixed stops"
            );
        }

        for (Integer segmentId : originalSegmentIds.keySet()) {
            List<UUID> requestedInSegment =
                    requestedSegments.get(segmentId);

            if (requestedInSegment == null
                    || requestedInSegment.size()
                    != originalSegmentIds.get(segmentId).size()
                    || !new HashSet<>(requestedInSegment)
                    .equals(originalSegmentIds.get(segmentId))) {

                throw new IllegalArgumentException(
                        "Pending stops cannot cross fixed stops"
                );
            }
        }

        // Construct the complete proposed route.
        List<RideStop> finalRoute = new ArrayList<>();
        Set<Integer> processedSegments = new HashSet<>();

        for (RideStop stop : stops) {

            if (stop.getStatus() != StopStatus.PENDING) {
                finalRoute.add(stop);
                continue;
            }

            Integer segmentId = originalSegments.get(stop.getId());

            if (processedSegments.add(segmentId)) {
                for (UUID stopId : requestedSegments.get(segmentId)) {
                    finalRoute.add(stopMap.get(stopId));
                }
            }
        }

        // Calculate positions in the complete proposed route.
        Map<UUID, Integer> finalPositions = new HashMap<>();

        for (int i = 0; i < finalRoute.size(); i++) {
            finalPositions.put(finalRoute.get(i).getId(), i);
        }

        // Every passenger must be picked up before being dropped off.
        Map<UUID, List<RideStop>> bookingStops = stops.stream()
                .collect(Collectors.groupingBy(RideStop::getBookingId));

        for (List<RideStop> bookingStopList : bookingStops.values()) {

            RideStop pickup = bookingStopList.stream()
                    .filter(stop -> stop.getStopType() == StopType.PICKUP)
                    .findFirst()
                    .orElse(null);

            RideStop drop = bookingStopList.stream()
                    .filter(stop -> stop.getStopType() == StopType.DROP)
                    .findFirst()
                    .orElse(null);

            if (pickup != null && drop != null
                    && finalPositions.get(pickup.getId())
                    >= finalPositions.get(drop.getId())) {

                throw new IllegalArgumentException(
                        "Passenger pickup must precede drop-off"
                );
            }
        }

        // Preserve original sequence positions within each segment.
        Map<Integer, List<Integer>> segmentPositions = new HashMap<>();

        for (RideStop stop : pendingStops) {
            Integer segmentId = originalSegments.get(stop.getId());

            segmentPositions
                    .computeIfAbsent(segmentId, key -> new ArrayList<>())
                    .add(stop.getSequence());
        }

        for (List<Integer> positions : segmentPositions.values()) {
            positions.sort(Integer::compareTo);
        }

        // Temporarily shift pending stops to avoid unique sequence collisions.
        int maxSequence = stops.stream()
                .mapToInt(RideStop::getSequence)
                .max()
                .orElse(0);

        int offset = maxSequence + stops.size() + 1;

        for (RideStop stop : pendingStops) {
            stop.setSequence(stop.getSequence() + offset);
        }

        rideStopRepository.saveAllAndFlush(pendingStops);

        // Assign the requested order to the original sequence slots.
        for (Map.Entry<Integer, List<UUID>> entry
                : requestedSegments.entrySet()) {

            List<Integer> availablePositions =
                    segmentPositions.get(entry.getKey());

            for (int i = 0; i < entry.getValue().size(); i++) {
                RideStop stop = stopMap.get(entry.getValue().get(i));

                stop.setSequence(availablePositions.get(i));
            }
        }

        rideStopRepository.saveAll(pendingStops);
    }
}





//
//Good question. `reorderStops()` is one of the most important methods in our ride-pooling system because it controls how the driver's route changes when new passengers join.
//
//Let's understand it step by step using your actual implementation.
//
//        # 1. What problem does `reorderStops()` solve?
//
//Imagine a shared ride with two passengers.
//
//        Initially, the route is:
//
//        | Sequence | Stop   | Passenger |
//        | -------- | ------ | --------- |
//        | 1        | Pickup | A         |
//        | 2        | Drop   | A         |
//        | 3        | Pickup | B         |
//        | 4        | Drop   | B         |
//
//Now the matching engine calculates that a better route is:
//
//        | New sequence | Stop   | Passenger |
//        | ------------ | ------ | --------- |
//        | 1            | Pickup | A         |
//        | 2            | Pickup | B         |
//        | 3            | Drop   | A         |
//        | 4            | Drop   | B         |
//
//The method receives the new order as a list of stop IDs:
//
//        ```
//reorderStops(rideId, List.of(
//                     pickupAId,
//             pickupBId,
//             dropAId,
//             dropBId
//             ));
//        ```
//
//Its job is to update the database sequence numbers to reflect this new order.
//
//# 2. Understanding the implementation
//
//Let's break down the method.
//
//        ## Part 1: Retrieve the ride and its stops
//
//```
//        if (!rideRepository.existsById(rideId)) {
//        throw new ResourceNotFoundException("Ride not found");
//}
//
//List<RideStop> stops =
//        rideStopRepository.findByRideIdOrderBySequenceAsc(rideId);
//```
//
//First, we verify that the ride exists.
//
//Then we retrieve all its stops in their current sequence order.
//
//For example:
//
//        ```
//A-PICKUP  → 1
//A-DROP    → 2
//B-PICKUP  → 3
//B-DROP    → 4
//        ```
//
//        ## Part 2: Identify pending stops
//
//```
//List<RideStop> pendingStops = stops.stream()
//        .filter(stop -> stop.getStatus() == StopStatus.PENDING)
//        .toList();
//```
//
//Why do we do this?
//
//Because we should not freely change stops that have already been reached or completed.
//
//Consider:
//
//        | Stop     | Status    |
//        | -------- | --------- |
//        | A pickup | COMPLETED |
//        | A drop   | PENDING   |
//        | B pickup | PENDING   |
//        | B drop   | PENDING   |
//
//The driver has already picked up A.
//
//We must preserve that completed stop.
//
//Only the pending stops can be rearranged.
//
//## Part 3: Validate the requested order
//
//```
//Set<UUID> requestedIds = new HashSet<>(orderedStopIds);
//
//Set<UUID> pendingIds = pendingStops.stream()
//        .map(RideStop::getId)
//        .collect(Collectors.toSet());
//```
//
//Here we create two sets:
//
//        - `requestedIds`: stops provided by the route optimizer.
//        - `pendingIds`: stops that are actually eligible for reordering.
//
//        Then:
//
//        ```
//        if (requestedIds.size() != orderedStopIds.size()
//        || !requestedIds.equals(pendingIds)) {
//        throw new IllegalArgumentException(
//            "Provide every pending stop exactly once"
//);
//}
//        ```
//
//This prevents three problems:
//
//        1. A stop is missing from the new route.
//2. A stop appears twice.
//        3. An unrelated or already completed stop is included.
//
//For example:
//
//Valid order
//
//Accepted
//
//A, B, C, D → all four pending stops exactly once
//
//Missing stop
//
//Rejected
//
//A, B, C → D is missing
//
//Duplicate stop
//
//Rejected
//
//A, B, B, C, D → B appears twice
//
//## Part 4: Preserve passenger pickup-before-drop ordering
//
//This is a critical business rule.
//
//        ```
//Map<UUID, List<RideStop>> bookingStops = pendingStops.stream()
//        .collect(Collectors.groupingBy(RideStop::getBookingId));
//```
//
//We group stops by their booking ID.
//
//For example:
//
//        ```
//Booking A:
//A-PICKUP
//A-DROP
//
//Booking B:
//B-PICKUP
//B-DROP
//```
//
//Then we locate each booking's pickup and drop:
//
//        ```
//RideStop pickup = bookingStopList.stream()
//        .filter(s -> s.getStopType() == StopType.PICKUP)
//        .findFirst()
//        .orElse(null);
//
//RideStop drop = bookingStopList.stream()
//        .filter(s -> s.getStopType() == StopType.DROP)
//        .findFirst()
//        .orElse(null);
//```
//
//We compare their positions in the proposed route:
//
//        ```
//        if (pickup != null && drop != null
//        && newPositions.get(pickup.getId())
//        >= newPositions.get(drop.getId())) {
//
//        throw new IllegalArgumentException(
//            "Passenger pickup must precede drop-off"
//);
//}
//        ```
//
//For example:
//
//Valid
//
//        Accepted
//
//```
//        1. A-PICKUP
//2. B-PICKUP
//3. A-DROP
//4. B-DROP
//```
//
//Invalid
//
//        Rejected
//
//```
//        1. A-DROP
//2. A-PICKUP
//3. B-PICKUP
//4. B-DROP
//```
//
//Passenger A cannot be dropped before being picked up.
//
//## Part 5: Why do we temporarily change sequence numbers?
//
//This is probably the most interesting part.
//
//Your database has this constraint:
//
//        ```
//@UniqueConstraint(
//        name = "uk_ride_stop_sequence",
//        columnNames = {"ride_id", "sequence_number"}
//)
//```
//
//It means two stops belonging to the same ride cannot have the same sequence number.
//
//Suppose the current order is:
//
//        ```
//A = 1
//B = 2
//C = 3
//D = 4
//        ```
//
//And we want:
//
//        ```
//B = 1
//C = 2
//A = 3
//D = 4
//        ```
//
//If we directly change A from 1 to 3, the database may reject it because C already occupies sequence 3.
//
//We therefore use temporary sequence numbers.
//
//        ```
//int maxSequence = stops.stream()
//        .mapToInt(RideStop::getSequence)
//        .max()
//        .orElse(0);
//
//int offset = maxSequence + stops.size() + 1;
//```
//
//Here:
//
//        ```
//maxSequence = 4
//number of stops = 4
//
//offset = 4 + 4 + 1
//offset = 9
//        ```
//
//Then:
//
//        ```
//        for (RideStop stop : pendingStops) {
//        stop.setSequence(stop.getSequence() + offset);
//        }
//
//        rideStopRepository.saveAllAndFlush(pendingStops);
//```
//
//The sequence numbers temporarily become:
//
//        ```
//A = 10
//B = 11
//C = 12
//D = 13
//        ```
//
//Now positions 1, 2, 3 and 4 are free.
//
//Finally, we assign the new positions:
//
//        ```
//        for (int i = 0; i < orderedStopIds.size(); i++) {
//RideStop stop = stopMap.get(orderedStopIds.get(i));
//    stop.setSequence(availablePositions.get(i));
//        }
//        ```
//
//The result becomes:
//
//        ```
//B = 1
//C = 2
//A = 3
//D = 4
//        ```
//
//And we save:
//
//        ```
//        rideStopRepository.saveAll(pendingStops);
//```
//
//The new order is persisted
//
//# 3. One important limitation in our current implementation
//
//I want you to understand this before we proceed.
//
//Our current method is a first version, not yet production-ready.
//
//There are three issues we must address:
//
//        1. Completed stop preservation: Our code preserves completed stops, but it doesn't fully validate their positions against the newly reordered pending stops.
//        2. Concurrency: Two simultaneous route updates could interfere. We will need ride-level database locking.
//3. Route optimization: This method only changes stop ordering. It does not calculate the optimal route, travel time or detour. That belongs in `RouteOptimizationService`.
//
//Also, `reorderStops()` must run within a transaction, which it currently does. The temporary sequence update is flushed before assigning the final positions.
//
//# 4. Check your understanding
//
//Before moving forward, try these three questions.
//
//        Question 1. Why do we temporarily change sequence numbers before assigning the new order?
//
//To improve route distance
//
//To avoid unique constraint conflicts
//
//To update passenger status
//
//Correct — Temporary positions free the existing sequence values so the new order can be persisted without collisions.
//
//Question 2. Can a passenger's DROP stop come before their PICKUP stop?
//
//Yes, if the route is shorter
//
//Only for mid-ride joins
//
//No, pickup must always come first
//
//Correct — Every booking must preserve pickup-before-drop ordering.
//
//Question 3. Does reorderStops() calculate the shortest route?
//
//Yes, using GPS coordinates
//
//No, it only persists a supplied stop order
//
//Yes, using Redis GEO
//
//Correct — The optimizer must calculate and supply the proposed order. This method validates and saves it.
//
//Check answers
//
//Score: 3/3
//
//My goal is that you understand this logic well enough to explain it in a backend interview, not just use the method in your project.