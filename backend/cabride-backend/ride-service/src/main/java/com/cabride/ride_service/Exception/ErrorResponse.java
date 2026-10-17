package com.cabride.ride_service.Exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String message ;
    private LocalDateTime timestamp;

}
