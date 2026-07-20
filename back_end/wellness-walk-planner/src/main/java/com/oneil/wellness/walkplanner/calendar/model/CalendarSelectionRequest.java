package com.oneil.wellness.walkplanner.calendar.model;

import java.util.List;

public record CalendarSelectionRequest(List<String> calendarIds) {
    public CalendarSelectionRequest { calendarIds = calendarIds == null ? List.of() : List.copyOf(calendarIds); }
}
