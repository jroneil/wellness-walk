package com.oneil.wellness.walkplanner.calendar.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;

public interface CalendarService {
    Optional<CalendarEvent> findBusyConflict(
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            List<CalendarEvent> events);
}
