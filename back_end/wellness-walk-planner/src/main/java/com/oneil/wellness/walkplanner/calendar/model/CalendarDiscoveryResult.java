package com.oneil.wellness.walkplanner.calendar.model;

import java.time.Instant;
import java.util.List;

public record CalendarDiscoveryResult(List<CalendarDescriptor> calendars, String status, String message,
        Instant discoveredAt) {
    public CalendarDiscoveryResult {
        calendars = calendars == null ? List.of() : List.copyOf(calendars);
    }
}
