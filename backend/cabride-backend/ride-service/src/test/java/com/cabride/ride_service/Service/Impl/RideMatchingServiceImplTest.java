
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Dto.Request.MatchRideRequest;
import com.cabride.ride_service.Dto.Response.MatchRideResponse;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideRequest;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.RideRequestStatus;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideRequestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideMatchingServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private RideBookingRepository rideBookingRepository;

    @InjectMocks
    private RideMatchingServiceImpl rideMatchingService;

    private UUID requestId;
    private UUID passengerId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        passengerId = UUID.randomUUID();
    }

    // Common helper: creates a valid passenger ride request.
    private RideRequest createRideRequest() {

        RideRequest request = new RideRequest();

        request.setId(requestId);
        request.setPassengerId(passengerId);
        request.setStatus(RideRequestStatus.REQUESTED);

        request.setPickupLat(26.4499);
        request.setPickupLon(80.3319);

        request.setDestinationLat(26.5123);
        request.setDestinationLon(80.2329);

        return request;
    }

    // Common helper: creates an eligible shared ride.
    private Ride createRide(UUID rideId) {

        Ride ride = new Ride();

        ride.setId(rideId);
        ride.setDriverId(UUID.randomUUID());

        ride.setRideType(RideType.SHARED);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAvailableSeats(2);

        ride.setPickupLatitude(26.4499);
        ride.setPickupLongitude(80.3319);

        ride.setDestinationLatitude(26.5123);
        ride.setDestinationLongitude(80.2329);

        return ride;
    }

    // Common helper: configures repository responses for a valid request.
    private void mockValidRequest(RideRequest request, List<Ride> candidates) {

        when(rideRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        when(rideRepository
                .findByRideTypeAndStatusInAndAvailableSeatsGreaterThan(
                        eq(RideType.SHARED),
                        anyList(),
                        eq(0)
                ))
                .thenReturn(candidates);

        when(rideBookingRepository.findByPassengerIdAndStatusIn(
                eq(passengerId),
                anyList()
        )).thenReturn(List.of());
    }

    // 1. Successful matching.
    @Test
    void findMatchingRides_shouldReturnMatchingSharedRides() {

        RideRequest request = createRideRequest();

        UUID rideId = UUID.randomUUID();
        Ride candidate = createRide(rideId);

        mockValidRequest(request, List.of(candidate));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(1, result.size());
        assertEquals(rideId, result.get(0).getRideId());
        assertEquals(RideType.SHARED, result.get(0).getRideType());
        assertEquals(RideStatus.ACCEPTED, result.get(0).getRideStatus());
    }

    // 2. Missing request and invalid request status.
    @Test
    void findMatchingRides_shouldRejectInvalidOrMissingRequests() {

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        // Scenario A: request does not exist.
        when(rideRequestRepository.findById(requestId))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> rideMatchingService.findMatchingRides(matchRequest)
        );

        // Scenario B: request exists but is not REQUESTED.
        RideRequest invalidRequest = createRideRequest();
        invalidRequest.setStatus(RideRequestStatus.MATCHED);

        when(rideRequestRepository.findById(requestId))
                .thenReturn(Optional.of(invalidRequest));

        assertThrows(
                IllegalStateException.class,
                () -> rideMatchingService.findMatchingRides(matchRequest)
        );
    }

    // 3. Existing active bookings should exclude already booked rides.
    @Test
    void findMatchingRides_shouldExcludeAlreadyBookedRides() {

        RideRequest request = createRideRequest();

        UUID existingRideId = UUID.randomUUID();
        UUID newRideId = UUID.randomUUID();

        Ride existingRide = createRide(existingRideId);
        Ride newRide = createRide(newRideId);

        RideBooking activeBooking = new RideBooking();
        activeBooking.setRideId(existingRideId);
        activeBooking.setPassengerId(passengerId);
        activeBooking.setStatus(BookingStatus.CONFIRMED);

        when(rideRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        when(rideRepository
                .findByRideTypeAndStatusInAndAvailableSeatsGreaterThan(
                        eq(RideType.SHARED),
                        anyList(),
                        eq(0)
                ))
                .thenReturn(List.of(existingRide, newRide));

        when(rideBookingRepository.findByPassengerIdAndStatusIn(
                eq(passengerId),
                anyList()
        )).thenReturn(List.of(activeBooking));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(1, result.size());
        assertEquals(newRideId, result.get(0).getRideId());
    }

    // 4. Pickup distance filtering.
    @Test
    void findMatchingRides_shouldFilterByPickupDistance() {

        RideRequest request = createRideRequest();

        UUID nearbyRideId = UUID.randomUUID();
        UUID distantRideId = UUID.randomUUID();

        Ride nearbyRide = createRide(nearbyRideId);

        Ride distantRide = createRide(distantRideId);

        // Move pickup approximately 10 km away.
        distantRide.setPickupLongitude(80.4319);

        mockValidRequest(request, List.of(nearbyRide, distantRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(1, result.size());
        assertEquals(nearbyRideId, result.get(0).getRideId());
    }

    // 5. Destination distance filtering.
    @Test
    void findMatchingRides_shouldFilterByDestinationDistance() {

        RideRequest request = createRideRequest();

        UUID nearbyRideId = UUID.randomUUID();
        UUID distantRideId = UUID.randomUUID();

        Ride nearbyRide = createRide(nearbyRideId);

        Ride distantRide = createRide(distantRideId);

        // Keep pickup identical but move destination approximately 10 km away.
        distantRide.setDestinationLongitude(80.3329);
        distantRide.setDestinationLatitude(26.5123);

        mockValidRequest(request, List.of(nearbyRide, distantRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(1, result.size());
        assertEquals(nearbyRideId, result.get(0).getRideId());
    }

    // 6. Invalid and null coordinates.
    @Test
    void findMatchingRides_shouldRejectInvalidCoordinates() {

        RideRequest request = createRideRequest();

        UUID rideId = UUID.randomUUID();
        Ride invalidRide = createRide(rideId);

        // A null coordinate must not cause NullPointerException.
        invalidRide.setPickupLatitude(null);

        mockValidRequest(request, List.of(invalidRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertTrue(result.isEmpty());

        // An invalid passenger coordinate should be rejected.
        request.setPickupLat(95.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> rideMatchingService.findMatchingRides(matchRequest)
        );
    }

    // 7. Direction compatibility.
    @Test
    void findMatchingRides_shouldFilterByDirectionCompatibility() {

        RideRequest request = createRideRequest();

        // Passenger travels east.
        request.setPickupLat(26.0);
        request.setPickupLon(80.0);
        request.setDestinationLat(26.0);
        request.setDestinationLon(80.03);

        UUID rideId = UUID.randomUUID();
        Ride oppositeDirectionRide = createRide(rideId);

        // Existing ride travels west over approximately the same corridor.
        oppositeDirectionRide.setPickupLatitude(26.0);
        oppositeDirectionRide.setPickupLongitude(80.03);
        oppositeDirectionRide.setDestinationLatitude(26.0);
        oppositeDirectionRide.setDestinationLongitude(80.0);

        mockValidRequest(request, List.of(oppositeDirectionRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertTrue(result.isEmpty());
    }

    // 8. Candidate ranking.
    @Test
    void findMatchingRides_shouldRankCandidatesByCompatibilityScore() {

        RideRequest request = createRideRequest();

        UUID bestRideId = UUID.randomUUID();
        UUID secondRideId = UUID.randomUUID();

        // Perfect geographic and directional match.
        Ride bestRide = createRide(bestRideId);

        // Slightly less compatible candidate.
        Ride secondRide = createRide(secondRideId);
        secondRide.setPickupLongitude(80.3419);
        secondRide.setDestinationLongitude(80.2429);

        mockValidRequest(request, List.of(secondRide, bestRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(2, result.size());

        // Best candidate must appear first.
        assertEquals(bestRideId, result.get(0).getRideId());
        assertEquals(secondRideId, result.get(1).getRideId());
    }

    // 9. No geographically compatible rides.
    @Test
    void findMatchingRides_shouldReturnEmptyWhenNoCompatibleRidesExist() {

        RideRequest request = createRideRequest();

        UUID rideId = UUID.randomUUID();
        Ride distantRide = createRide(rideId);

        // Move the pickup far outside the allowed radius.
        distantRide.setPickupLongitude(81.0);

        mockValidRequest(request, List.of(distantRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 10. Multiple candidates with equal compatibility scores.
    @Test
    void findMatchingRides_shouldHandleMultipleCandidatesAndEqualScores() {

        RideRequest request = createRideRequest();

        UUID firstRideId = UUID.randomUUID();
        UUID secondRideId = UUID.randomUUID();

        Ride firstRide = createRide(firstRideId);
        Ride secondRide = createRide(secondRideId);

        // Both rides have identical coordinates and compatibility.
        mockValidRequest(request, List.of(firstRide, secondRide));

        MatchRideRequest matchRequest = new MatchRideRequest();
        matchRequest.setRideRequestId(requestId);

        List<MatchRideResponse> result =
                rideMatchingService.findMatchingRides(matchRequest);

        assertEquals(2, result.size());

        List<UUID> returnedRideIds = result.stream()
                .map(MatchRideResponse::getRideId)
                .toList();

        assertTrue(returnedRideIds.contains(firstRideId));
        assertTrue(returnedRideIds.contains(secondRideId));
    }

}
