package com.oneil.wellness.walkplanner.calendar.model;

import java.time.Instant;
import java.util.List;

public record CalendarSyncResult(List<CalendarEvent> events, List<CalendarSyncError> errors,
        List<CalendarSynchronizationDetail> calendars, Instant synchronizedAt) {
    public CalendarSyncResult {
        events = events == null ? List.of() : List.copyOf(events);
        errors = errors == null ? List.of() : List.copyOf(errors);
        calendars = calendars == null ? List.of() : List.copyOf(calendars);
    }

    public CalendarSyncResult(List<CalendarEvent> events, List<CalendarSyncError> errors, Instant synchronizedAt) {
        this(events, errors, List.of(), synchronizedAt);
    }

    public boolean isPartial() { return !events.isEmpty() && !errors.isEmpty(); }
}
