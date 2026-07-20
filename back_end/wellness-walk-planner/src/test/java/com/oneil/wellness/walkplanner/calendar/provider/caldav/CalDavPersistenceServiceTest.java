package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.oneil.wellness.walkplanner.calendar.credential.ProviderCredentialStore;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType;
import com.oneil.wellness.walkplanner.calendar.persistence.CalendarProviderConnection;
import com.oneil.wellness.walkplanner.calendar.persistence.ProviderConnectionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CalDavPersistenceServiceTest {
    @Test void explicitEnvironmentCredentialsBootstrapAnEmptyConnection() {
        var connections = mock(ProviderConnectionService.class);
        var credentials = mock(ProviderCredentialStore.class);
        var connection = new CalendarProviderConnection(CalendarProviderType.CALDAV, "CalDAV", true);
        when(connections.find(CalendarProviderType.CALDAV)).thenReturn(Optional.empty());
        when(connections.getOrCreate(CalendarProviderType.CALDAV, "CalDAV", true)).thenReturn(connection);
        when(credentials.readSecret(connection.getId(), "username")).thenReturn(Optional.empty());
        when(credentials.readSecret(connection.getId(), "password")).thenReturn(Optional.empty());

        new CalDavPersistenceService(connections, credentials).resolve(config("wellness", "first-password"));

        verify(credentials).saveSecret(connection.getId(), "username", "wellness");
        verify(credentials).saveSecret(connection.getId(), "password", "first-password");
    }

    @Test void changedExplicitCredentialsRotateWithoutTouchingCalendarSelection() {
        var connections = mock(ProviderConnectionService.class);
        var credentials = mock(ProviderCredentialStore.class);
        var connection = new CalendarProviderConnection(CalendarProviderType.CALDAV, "CalDAV", true);
        connection.configure(true, "CalDAV", "http://radicale:5232/", "", "America/New_York");
        when(connections.find(CalendarProviderType.CALDAV)).thenReturn(Optional.of(connection));
        when(credentials.readSecret(connection.getId(), "username")).thenReturn(Optional.of("wellness"));
        when(credentials.readSecret(connection.getId(), "password")).thenReturn(Optional.of("old-password"));

        new CalDavPersistenceService(connections, credentials).resolve(config("wellness", "new-password"));

        verify(credentials, never()).rotateSecret(connection.getId(), "username", "wellness");
        verify(credentials).rotateSecret(connection.getId(), "password", "new-password");
        verify(connections, never()).persistSelections(any(), any(), any());
    }

    @Test void persistedCredentialsRestoreWhenEnvironmentValuesAreAbsent() {
        var connections = mock(ProviderConnectionService.class);
        var credentials = mock(ProviderCredentialStore.class);
        var connection = new CalendarProviderConnection(CalendarProviderType.CALDAV, "CalDAV", true);
        connection.configure(true, "CalDAV", "https://calendar.example.test/", "/home/", "UTC");
        when(connections.find(CalendarProviderType.CALDAV)).thenReturn(Optional.of(connection));
        when(credentials.readSecret(connection.getId(), "username")).thenReturn(Optional.of("stored-user"));
        when(credentials.readSecret(connection.getId(), "password")).thenReturn(Optional.of("stored-password"));
        var config = config("", ""); config.setServerUrl(""); config.setCalendarPath("");

        new CalDavPersistenceService(connections, credentials).resolve(config);

        assertEquals("stored-user", config.getUsername());
        assertEquals("stored-password", config.getPassword());
        assertEquals("https://calendar.example.test/", config.getServerUrl());
        assertEquals("/home/", config.getCalendarPath());
    }

    private CalDavConfiguration config(String username, String password) {
        var config = new CalDavConfiguration();
        config.setEnabled(true); config.setServerUrl("http://radicale:5232/");
        config.setUsername(username); config.setPassword(password); config.setDefaultTimezone("America/New_York");
        return config;
    }
}
