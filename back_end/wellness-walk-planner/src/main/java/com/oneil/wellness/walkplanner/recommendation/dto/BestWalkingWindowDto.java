package com.oneil.wellness.walkplanner.recommendation.dto;

import java.util.List;

import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

public record BestWalkingWindowDto(
        String startTime,
        String endTime,
        int score,
        WalkingRating rating,
        String ratingLabel,
        String summary,
        List<String> positiveReasons,
        List<String> warnings) {
}
