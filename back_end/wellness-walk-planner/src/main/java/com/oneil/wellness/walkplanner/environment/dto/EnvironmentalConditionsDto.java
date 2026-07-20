package com.oneil.wellness.walkplanner.environment.dto;

import java.math.BigDecimal;

public record EnvironmentalConditionsDto(
        BigDecimal actualTemperature,
        BigDecimal feelsLikeTemperature,
        String temperatureUnit,
        String feelsLikeMethod,
        BigDecimal aqi,
        String aqiCategory,
        String aqiObservationTime,
        String aqiSource,
        BigDecimal uvIndex,
        String uvCategory,
        String uvObservationOrForecastTime,
        String uvSource,
        String sunrise,
        String sunset,
        String daylightStatus,
        Integer remainingDaylightMinutes) {
}
