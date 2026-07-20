package com.oneil.wellness.walkplanner.calendar;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.oneil.wellness.walkplanner.calendar.model.*;
import com.oneil.wellness.walkplanner.calendar.provider.caldav.CalDavException;
import com.oneil.wellness.walkplanner.calendar.provider.manual.ManualCalendarProvider;
import com.oneil.wellness.walkplanner.calendar.service.CalendarSyncService;

class CalendarProviderFrameworkTest {
    @Test void registryListsManualAndFindsProviders() {
        var manual = new ManualCalendarProvider(); var caldav = provider(CalendarProviderType.CALDAV, false, List.of(), null);
        var registry = new CalendarProviderRegistry(List.of(manual, caldav));
        assertEquals(2, registry.list().size()); assertSame(caldav, registry.require(CalendarProviderType.CALDAV));
        assertEquals(List.of(manual), registry.enabled());
    }

    @Test void registryRejectsDuplicateTypes() {
        assertThrows(IllegalArgumentException.class, () -> new CalendarProviderRegistry(List.of(new ManualCalendarProvider(), new ManualCalendarProvider())));
    }

    @Test void syncDeduplicatesProviderAwareOccurrences() {
        var start = OffsetDateTime.parse("2026-07-20T10:00:00-04:00");
        var event = event("uid", start, CalendarProviderType.CALDAV);
        var service = new CalendarSyncService(new CalendarProviderRegistry(List.of(provider(CalendarProviderType.CALDAV, true, List.of(event, event), null))));
        assertEquals(1, service.synchronize(start.minusHours(1), start.plusHours(2), List.of()).events().size());
    }

    @Test void syncReturnsManualEventsWhenExternalProviderFails() {
        var start = OffsetDateTime.parse("2026-07-20T10:00:00-04:00"); var manual = event("manual", start, CalendarProviderType.MANUAL);
        var service = new CalendarSyncService(new CalendarProviderRegistry(List.of(provider(CalendarProviderType.CALDAV, true, List.of(), new CalDavException("TIMEOUT", "CalDAV request timed out.")))));
        var result = service.synchronize(start.minusHours(1), start.plusHours(2), List.of(manual));
        assertEquals(List.of(manual), result.events()); assertEquals("TIMEOUT", result.errors().getFirst().code()); assertTrue(result.isPartial());
    }

    private CalendarProvider provider(CalendarProviderType type, boolean enabled, List<CalendarEvent> events, RuntimeException failure) {
        return new CalendarProvider() {
            public CalendarProviderType getType() { return type; } public String getDisplayName() { return type.name(); }
            public boolean isEnabled() { return enabled; } public CalendarConnectionStatus getConnectionStatus() { return enabled ? CalendarConnectionStatus.CONNECTED : CalendarConnectionStatus.DISABLED; }
            public CalendarConnectionResult testConnection() { return new CalendarConnectionResult(getConnectionStatus(), "test"); }
            public List<CalendarEvent> fetchEvents(OffsetDateTime start, OffsetDateTime end) { if (failure != null) throw failure; return events; }
            public CalendarProviderCapabilities getCapabilities() { return CalendarProviderCapabilities.readOnly(false, false); }
        };
    }
    private CalendarEvent event(String uid, OffsetDateTime start, CalendarProviderType type) {
        return new CalendarEvent(type + ":" + uid, "Event", start, start.plusMinutes(30), true,
                type == CalendarProviderType.CALDAV ? CalendarSource.CALDAV : CalendarSource.MANUAL, type, uid,
                type.name().toLowerCase(), start.toInstant().toString(), start.getOffset().toString(), false);
    }
}
