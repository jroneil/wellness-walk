package com.oneil.wellness.walkplanner.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

public record DailyOutlookDto(
        String date,
        String dayName,
        String iconUrl,
        String shortForecast,
        BigDecimal highTemperature,
        BigDecimal lowTemperature,
        String temperatureUnit,
        BigDecimal precipitationProbability,
        Integer representativeScore,
        WalkingRating rating,
        String ratingLabel,
        String bestAvailableTime,
        String summary,
        List<String> environmentalWarnings) {
}
