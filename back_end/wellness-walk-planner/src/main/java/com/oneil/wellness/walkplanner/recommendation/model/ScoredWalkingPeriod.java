package com.oneil.wellness.walkplanner.recommendation.model;

import java.util.List;

public record ScoredWalkingPeriod(
        String startTime,
        Integer score,
        WalkingRating rating,
        boolean recommended,
        Integer temperatureScore,
        Integer precipitationScore,
        Integer windScore,
        Integer humidityScore,
        Integer daylightScore,
        Integer airQualityScore,
        Integer uvScore,
        java.math.BigDecimal feelsLikeTemperature,
        String feelsLikeMethod,
        List<String> reasons,
        List<String> warnings) {

    public boolean scorable() {
        return score != null;
    }
}
