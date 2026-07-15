package com.oneil.wellness.walkplanner.dto;

import java.math.BigDecimal;

public record CurrentWeatherSummary(
        BigDecimal temperature,
        String temperatureUnit,
        BigDecimal feelsLike,
        BigDecimal humidity,
        BigDecimal windSpeed,
        String windDirection,
        String weatherCondition,
        String iconUrl,
        String observationTime,
        String dataType) {
}
