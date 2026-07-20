package com.oneil.wellness.walkplanner.calendar.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.calendar.CalendarProvider;
import com.oneil.wellness.walkplanner.calendar.model.*;

class CalendarProviderDtoTest {
    @Test
    void exposesInstallationAuthorizationSelectionAndSyncStateWithoutSecrets() {
        Instant attempted = Instant.parse("2026-07-20T12:00:00Z");
        Instant successful = Instant.parse("2026-07-20T12:01:00Z");
        CalendarProviderDto dto = CalendarProviderDto.from(new FixtureProvider(
                CalendarProviderType.GOOGLE, true, CalendarConnectionStatus.AUTHORIZATION_REQUIRED,
                4, 2, new CalendarProviderSyncStatus(attempted, successful, "SUCCESS", "Safe", List.of())));

        assertTrue(dto.installationConfigured());
        assertTrue(dto.enabled());
        assertFalse(dto.connected());
        assertTrue(dto.authorizationRequired());
        assertEquals(4, dto.discoveredCalendarCount());
        assertEquals(2, dto.selectedCalendarCount());
        assertEquals(attempted, dto.lastAttemptedSyncAt());
        assertEquals(successful, dto.lastSuccessfulSyncAt());
        assertEquals(CalendarConnectionStatus.AUTHORIZATION_REQUIRED, dto.providerStatus());
        assertFalse(dto.toString().toLowerCase().contains("secret"));
    }

    @Test
    void disabledGoogleHasExplicitInstallationMessage() {
        CalendarProviderDto dto = CalendarProviderDto.from(new FixtureProvider(
                CalendarProviderType.GOOGLE, false, CalendarConnectionStatus.DISABLED, 0, 0,
                CalendarProviderSyncStatus.never()));
        assertFalse(dto.installationConfigured());
        assertEquals("Google Calendar is not configured for this Wellness Window installation.", dto.safeMessage());
    }

    private record FixtureProvider(CalendarProviderType type, boolean configured, CalendarConnectionStatus state,
            int discovered, int selected, CalendarProviderSyncStatus sync) implements CalendarProvider {
        public CalendarProviderType getType(){return type;}
        public String getDisplayName(){return type.name();}
        public boolean isEnabled(){return configured;}
        public boolean isInstallationConfigured(){return configured;}
        public CalendarConnectionStatus getConnectionStatus(){return state;}
        public CalendarConnectionResult testConnection(){return new CalendarConnectionResult(state,"Safe");}
        public List<CalendarEvent> fetchEvents(OffsetDateTime start,OffsetDateTime end){return List.of();}
        public CalendarProviderCapabilities getCapabilities(){return CalendarProviderCapabilities.readOnly(true,true);}
        public int getDiscoveredCalendarCount(){return discovered;}
        public int getSelectedCalendarCount(){return selected;}
        public CalendarProviderSyncStatus getSyncStatus(){return sync;}
    }
}
