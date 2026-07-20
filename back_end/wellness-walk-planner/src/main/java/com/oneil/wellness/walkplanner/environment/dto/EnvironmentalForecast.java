package com.oneil.wellness.walkplanner.environment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record EnvironmentalForecast(
        Map<String, HourlyEnvironment> hourlyByStartTime,
        Map<LocalDate, DailyEnvironment> dailyByDate) {

    public static EnvironmentalForecast empty() {
        return new EnvironmentalForecast(Map.of(), Map.of());
    }

    public record HourlyEnvironment(
            String startTime,
            BigDecimal uvIndex,
            String uvCategory,
            String uvObservationOrForecastTime,
            String uvSource,
            BigDecimal aqi,
            String aqiCategory,
            String aqiObservationTime,
            String aqiSource,
            String sunrise,
            String sunset,
            String daylightStatus,
            Integer remainingDaylightMinutes) {
    }

    public record DailyEnvironment(
            LocalDate date,
            BigDecimal uvIndexMax,
            String sunrise,
            String sunset,
            BigDecimal maxAqi) {
    }
}
