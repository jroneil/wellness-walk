package com.oneil.wellness.walkplanner.calendar.model;

import java.time.Instant;
import java.util.List;

public record CalendarProviderSyncStatus(Instant lastAttempt, Instant lastSuccess, String status, String message,
        List<CalendarSynchronizationDetail> calendars) {
    public CalendarProviderSyncStatus { calendars = calendars == null ? List.of() : List.copyOf(calendars); }
    public static CalendarProviderSyncStatus never() { return new CalendarProviderSyncStatus(null, null, "NEVER", "Not synchronized yet.", List.of()); }
}
