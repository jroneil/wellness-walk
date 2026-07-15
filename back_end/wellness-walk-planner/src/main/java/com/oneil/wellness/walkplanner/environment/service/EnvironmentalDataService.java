package com.oneil.wellness.walkplanner.environment.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.environment.client.OpenMeteoEnvironmentClient;
import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalForecast;

@Service
public class EnvironmentalDataService {

    private final OpenMeteoEnvironmentClient client;

    public EnvironmentalDataService(OpenMeteoEnvironmentClient client) {
        this.client = client;
    }

    public EnvironmentalForecast getEnvironmentalForecast(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return EnvironmentalForecast.empty();
        }
        return client.fetch(latitude, longitude);
    }
}
