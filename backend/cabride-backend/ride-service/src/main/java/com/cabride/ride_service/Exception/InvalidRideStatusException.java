package com.cabride.ride_service.Exception;

public class InvalidRideStatusException extends RuntimeException{
    public InvalidRideStatusException(String message){
        super(message);
    }
}
