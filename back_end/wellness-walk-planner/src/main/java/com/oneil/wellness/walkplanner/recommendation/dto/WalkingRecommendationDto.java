package com.oneil.wellness.walkplanner.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

public record WalkingRecommendationDto(
        String startTime,
        Integer score,
        WalkingRating rating,
        String ratingLabel,
        boolean recommended,
        Integer temperatureScore,
        Integer precipitationScore,
        Integer windScore,
        Integer humidityScore,
        Integer daylightScore,
        Integer airQualityScore,
        Integer uvScore,
        BigDecimal feelsLikeTemperature,
        String feelsLikeMethod,
        List<String> reasons,
        List<String> warnings) {
}
