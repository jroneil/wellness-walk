package com.oneil.wellness.walkplanner.recommendation.engine.model;

import java.time.OffsetDateTime;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;

public record CandidateWindow(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        HourlyForecastPeriod representativePeriod,
        int weatherScore,
        boolean available,
        CalendarEvent conflict,
        int preferenceScore,
        int overallScore) {

    public CandidateWindow withAvailability(boolean value, CalendarEvent event) {
        return new CandidateWindow(startTime, endTime, representativePeriod, weatherScore, value, event,
                preferenceScore, overallScore);
    }

    public CandidateWindow withScores(int preference, int overall) {
        return new CandidateWindow(startTime, endTime, representativePeriod, weatherScore, available, conflict,
                preference, overall);
    }
}
