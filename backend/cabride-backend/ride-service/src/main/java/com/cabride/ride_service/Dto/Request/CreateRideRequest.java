package com.cabride.ride_service.Dto.Request;


import com.cabride.ride_service.Enums.RideType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRideRequest {

        @NotNull(message = "Pickup latitude is required")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private Double pickupLat;

        @NotNull(message = "Pickup longitude is required")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private Double pickupLon;

        @NotNull(message = "Destination latitude is required")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private Double destinationLat;

        @NotNull(message = "Destination longitude is required")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private Double destinationLon;


}

