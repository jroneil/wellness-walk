package com.oneil.wellness.walkplanner.calendar;

import java.time.OffsetDateTime;
import java.util.List;

import com.oneil.wellness.walkplanner.calendar.model.*;

public interface CalendarProvider {
    CalendarProviderType getType();
    String getDisplayName();
    boolean isEnabled();
    CalendarConnectionStatus getConnectionStatus();
    CalendarConnectionResult testConnection();
    List<CalendarEvent> fetchEvents(OffsetDateTime start, OffsetDateTime end);
    default CalendarSyncResult synchronize(OffsetDateTime start, OffsetDateTime end) {
        return new CalendarSyncResult(fetchEvents(start, end), List.of(), java.time.Instant.now());
    }
    CalendarProviderCapabilities getCapabilities();
    default CalendarDiscoveryResult discoverCalendars() {
        return new CalendarDiscoveryResult(List.of(), "NOT_SUPPORTED", "Calendar discovery is not supported.", java.time.Instant.now());
    }
    default List<CalendarDescriptor> listCalendars() { return discoverCalendars().calendars(); }
    default List<CalendarDescriptor> selectCalendars(List<String> calendarIds) { return listCalendars(); }
    default List<CalendarDescriptor> getSelectedCalendars() { return listCalendars().stream().filter(CalendarDescriptor::selected).toList(); }
    default List<CalendarEvent> fetchEvents(CalendarDescriptor calendar, OffsetDateTime start, OffsetDateTime end) {
        return fetchEvents(start, end);
    }
    default CalendarProviderSyncStatus getSyncStatus() { return CalendarProviderSyncStatus.never(); }
    default boolean isInstallationConfigured() { return isEnabled(); }
    default int getDiscoveredCalendarCount() { return 0; }
    default int getSelectedCalendarCount() { return 0; }
    default void disconnect() { }
}
