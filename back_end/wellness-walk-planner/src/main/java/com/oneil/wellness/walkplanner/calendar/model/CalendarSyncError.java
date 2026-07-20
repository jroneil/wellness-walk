package com.oneil.wellness.walkplanner.calendar.model;

public record CalendarSyncError(CalendarProviderType providerType, String code, String message) {
}
