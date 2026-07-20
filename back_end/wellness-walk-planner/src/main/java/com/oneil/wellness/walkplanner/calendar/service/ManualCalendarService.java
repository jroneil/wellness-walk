package com.oneil.wellness.walkplanner.calendar.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;

@Service
public class ManualCalendarService implements CalendarService {

    @Override
    public Optional<CalendarEvent> findBusyConflict(
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            List<CalendarEvent> events) {
        if (windowStart == null || windowEnd == null || events == null) {
            return Optional.empty();
        }
        return events.stream()
                .filter(event -> event != null && event.overlaps(windowStart, windowEnd))
                .min(Comparator.comparing(CalendarEvent::startTime));
    }
}
