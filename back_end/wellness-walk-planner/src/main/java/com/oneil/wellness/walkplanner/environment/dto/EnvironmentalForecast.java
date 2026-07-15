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
            BigDecimal aqi,
            String sunrise,
            String sunset,
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
