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
        Integer aqiPenalty,
        Integer uvPenalty,
        BigDecimal feelsLikeTemperature,
        String feelsLikeSource,
        List<String> reasons,
        List<String> warnings) {
}
