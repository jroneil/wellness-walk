package com.oneil.wellness.walkplanner.calendar.dto;

import java.time.OffsetDateTime;
import java.util.List;
import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;

public record CalendarSyncRequest(OffsetDateTime start, OffsetDateTime end, List<CalendarEvent> manualEvents) {
    public List<CalendarEvent> normalizedManualEvents() { return manualEvents == null ? List.of() : manualEvents; }
}
