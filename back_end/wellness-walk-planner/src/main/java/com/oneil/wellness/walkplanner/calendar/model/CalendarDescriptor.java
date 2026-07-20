package com.oneil.wellness.walkplanner.calendar.model;

public record CalendarDescriptor(
        CalendarProviderType providerType,
        String calendarId,
        String displayName,
        String calendarUrl,
        boolean enabled,
        boolean readOnly,
        boolean supportsEvents,
        boolean selected,
        String color,
        String description) {
    public CalendarDescriptor withSelected(boolean value) {
        return new CalendarDescriptor(providerType, calendarId, displayName, calendarUrl, enabled, readOnly,
                supportsEvents, value, color, description);
    }
}
