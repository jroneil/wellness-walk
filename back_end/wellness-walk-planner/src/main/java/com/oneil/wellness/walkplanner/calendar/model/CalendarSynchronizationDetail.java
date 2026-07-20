package com.oneil.wellness.walkplanner.calendar.model;

public record CalendarSynchronizationDetail(CalendarProviderType providerType, String calendarId,
        String status, int eventCount, String errorCode, String message) {
}
