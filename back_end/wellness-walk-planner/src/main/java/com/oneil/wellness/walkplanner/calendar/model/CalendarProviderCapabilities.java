package com.oneil.wellness.walkplanner.calendar.model;

public record CalendarProviderCapabilities(boolean readEvents, boolean writeEvents, boolean discovery,
        boolean recurrence) {
    public static CalendarProviderCapabilities readOnly(boolean discovery, boolean recurrence) {
        return new CalendarProviderCapabilities(true, false, discovery, recurrence);
    }
}
