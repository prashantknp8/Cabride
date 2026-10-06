package com.cabride.ride_service.Exception;

public class ErrorResponse extends RuntimeException {
  public ErrorResponse(String message) {
    super(message);
  }
}
