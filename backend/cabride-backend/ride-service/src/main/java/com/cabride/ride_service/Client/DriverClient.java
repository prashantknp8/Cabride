package com.cabride.ride_service.Client;

import com.cabride.ride_service.Dto.Response.DriverResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverClient {
    private final WebClient.Builder webClientBuilder;

    public DriverResponse getDriverById(UUID driverId,String token){
        return webClientBuilder
                .build()
                .get()
                .uri("http://driver-service/api/drivers/{driverId}",driverId)
                .header("Authorization","Bearer " + token)
                .retrieve()
                .bodyToMono(DriverResponse.class)
                .block();
    }


    public DriverResponse getMyDriverProfile(String token) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://driver-service/api/drivers/me")
                .header("Authorization","Bearer " + token)
                .retrieve()
                .bodyToMono(DriverResponse.class)
                .block();
    }
}
