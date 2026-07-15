package com.oneil.wellness.walkplanner.environment.dto;

import java.math.BigDecimal;

public record EnvironmentalConditionsDto(
        BigDecimal feelsLikeTemperature,
        String feelsLikeSource,
        BigDecimal aqi,
        String aqiCategory,
        BigDecimal uvIndex,
        String uvCategory,
        String sunrise,
        String sunset,
        Integer remainingDaylightMinutes) {
}
