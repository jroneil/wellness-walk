package com.oneil.wellness.walkplanner.calendar.dto;

public record CalendarConflictDto(
        String eventId,
        String title,
        String startTime,
        String endTime,
        String source) {
}
