package com.oneil.wellness.walkplanner.recommendation.dto;

import java.util.List;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;

public record CalendarRecommendationRequest(
        RecommendationPreferencesDto preferences,
        List<CalendarEvent> calendarEvents) {

    public RecommendationPreferencesDto normalizedPreferences() {
        return preferences == null ? RecommendationPreferencesDto.defaults() : preferences.normalized();
    }

    public List<CalendarEvent> normalizedEvents() {
        return calendarEvents == null ? List.of() : calendarEvents;
    }
}
