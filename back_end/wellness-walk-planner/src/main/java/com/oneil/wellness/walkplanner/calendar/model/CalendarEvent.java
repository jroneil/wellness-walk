package com.oneil.wellness.walkplanner.calendar.model;

import java.time.OffsetDateTime;

public record CalendarEvent(
        String id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        boolean busy,
        CalendarSource source,
        CalendarProviderType providerType,
        String providerEventId,
        String calendarId,
        String occurrenceId,
        String timezone,
        Boolean allDay) {

    public CalendarEvent(String id, String title, OffsetDateTime startTime, OffsetDateTime endTime,
            boolean busy, CalendarSource source) {
        this(id, title, startTime, endTime, busy, source,
                source == CalendarSource.CALDAV ? CalendarProviderType.CALDAV : CalendarProviderType.MANUAL,
                id, source == CalendarSource.CALDAV ? "caldav" : "manual", startTime == null ? null : startTime.toInstant().toString(),
                startTime == null ? null : startTime.getOffset().toString(), false);
    }

    public boolean overlaps(OffsetDateTime windowStart, OffsetDateTime windowEnd) {
        return busy && startTime != null && endTime != null
                && startTime.isBefore(windowEnd)
                && endTime.isAfter(windowStart);
    }
}
