package com.cabride.ride_service.Exception;

public class UnauthorizedRideAccessException extends RuntimeException {
  public UnauthorizedRideAccessException(String message) {
    super(message);
  }
}
