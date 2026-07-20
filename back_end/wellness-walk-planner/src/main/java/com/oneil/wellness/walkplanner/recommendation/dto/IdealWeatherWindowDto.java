package com.oneil.wellness.walkplanner.recommendation.dto;

import com.oneil.wellness.walkplanner.calendar.dto.CalendarConflictDto;

public record IdealWeatherWindowDto(
        String startTime,
        String endTime,
        int score,
        String availability,
        CalendarConflictDto conflictingEvent) {
}
