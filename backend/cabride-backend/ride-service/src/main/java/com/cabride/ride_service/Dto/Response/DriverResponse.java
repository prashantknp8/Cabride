package com.cabride.ride_service.Dto.Response;

import lombok.Data;

import java.util.UUID;

@Data
public class DriverResponse {
    private UUID id;
    private UUID userId;
    private String licenseNumber;
    private Integer vehicleCapacity;
    private String status;


}
