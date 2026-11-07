package com.cabride.ride_service.Dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PassengerConsentRequest {

    @NotNull
    private Boolean approved;
}