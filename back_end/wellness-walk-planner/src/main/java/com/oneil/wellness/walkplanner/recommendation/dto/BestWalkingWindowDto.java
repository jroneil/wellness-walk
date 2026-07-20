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
        List<String> warnings,
        int durationMinutes,
        List<String> preferenceReasons,
        int minimumScore,
        boolean belowMinimumScore,
        String minimumScoreMessage,
        String availability,
        String selectionReason,
        com.oneil.wellness.walkplanner.calendar.dto.CalendarConflictDto conflictingEvent,
        IdealWeatherWindowDto idealWeatherWindow,
        int weatherScore,
        int availabilityScore,
        int preferenceScore,
        int overallScore,
        List<String> calendarReasons,
        String noAvailableReason) {

    public BestWalkingWindowDto(
            String startTime, String endTime, int score, WalkingRating rating, String ratingLabel, String summary,
            List<String> positiveReasons, List<String> warnings, int durationMinutes, List<String> preferenceReasons,
            int minimumScore, boolean belowMinimumScore, String minimumScoreMessage, String availability,
            String selectionReason, com.oneil.wellness.walkplanner.calendar.dto.CalendarConflictDto conflictingEvent,
            IdealWeatherWindowDto idealWeatherWindow) {
        this(startTime, endTime, score, rating, ratingLabel, summary, positiveReasons, warnings, durationMinutes,
                preferenceReasons, minimumScore, belowMinimumScore, minimumScoreMessage, availability, selectionReason,
                conflictingEvent, idealWeatherWindow, score, "AVAILABLE".equals(availability) ? 100 : 0, 0, score,
                List.of(), null);
    }

    public BestWalkingWindowDto(
            String startTime,
            String endTime,
            int score,
            WalkingRating rating,
            String ratingLabel,
            String summary,
            List<String> positiveReasons,
            List<String> warnings,
            int durationMinutes,
            List<String> preferenceReasons,
            int minimumScore,
            boolean belowMinimumScore,
            String minimumScoreMessage) {
        this(startTime, endTime, score, rating, ratingLabel, summary, positiveReasons, warnings,
                durationMinutes, preferenceReasons, minimumScore, belowMinimumScore, minimumScoreMessage,
                "AVAILABLE", "Highest available weather score.", null, null, score, 100, 0, score, List.of(), null);
    }
}
