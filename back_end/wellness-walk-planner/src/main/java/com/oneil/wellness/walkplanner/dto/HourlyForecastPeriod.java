package com.oneil.wellness.walkplanner.dto;

import java.math.BigDecimal;

import com.oneil.wellness.walkplanner.recommendation.dto.WalkingRecommendationDto;

public record HourlyForecastPeriod(
        String startTime,
        BigDecimal temperature,
        String temperatureUnit,
        String shortForecast,
        String iconUrl,
        BigDecimal precipitationProbability,
        BigDecimal humidity,
        BigDecimal windSpeed,
        String windDirection,
        Boolean isDaytime,
        BigDecimal feelsLikeTemperature,
        String feelsLikeSource,
        BigDecimal uvIndex,
        BigDecimal aqi,
        String sunrise,
        String sunset,
        Integer remainingDaylightMinutes,
        WalkingRecommendationDto walkingRecommendation) {

    public HourlyForecastPeriod withWalkingRecommendation(WalkingRecommendationDto recommendation) {
        return new HourlyForecastPeriod(
                startTime,
                temperature,
                temperatureUnit,
                shortForecast,
                iconUrl,
                precipitationProbability,
                humidity,
                windSpeed,
                windDirection,
                isDaytime,
                feelsLikeTemperature,
                feelsLikeSource,
                uvIndex,
                aqi,
                sunrise,
                sunset,
                remainingDaylightMinutes,
                recommendation);
    }

    public HourlyForecastPeriod withEnvironmentalData(
            BigDecimal feelsLikeTemperature,
            String feelsLikeSource,
            BigDecimal uvIndex,
            BigDecimal aqi,
            String sunrise,
            String sunset,
            Integer remainingDaylightMinutes) {
        return new HourlyForecastPeriod(
                startTime,
                temperature,
                temperatureUnit,
                shortForecast,
                iconUrl,
                precipitationProbability,
                humidity,
                windSpeed,
                windDirection,
                isDaytime,
                feelsLikeTemperature,
                feelsLikeSource,
                uvIndex,
                aqi,
                sunrise,
                sunset,
                remainingDaylightMinutes,
                walkingRecommendation);
    }
}
