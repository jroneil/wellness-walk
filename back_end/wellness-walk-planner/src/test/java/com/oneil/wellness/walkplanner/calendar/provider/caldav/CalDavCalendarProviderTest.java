package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.oneil.wellness.walkplanner.calendar.model.CalendarConnectionStatus;
import com.oneil.wellness.walkplanner.calendar.credential.EncryptedProviderCredentialStore.CredentialStoreException;

class CalDavCalendarProviderTest {
    @Test void connectionSuccessUpdatesStatus() {
        var client = mock(CalDavClient.class); var provider = provider(client, mock(CalDavEventMapper.class));
        assertEquals(CalendarConnectionStatus.CONNECTED, provider.testConnection().status());
    }
    @Test void authenticationFailureIsSanitized() {
        var client = mock(CalDavClient.class); doThrow(new CalDavException("AUTHENTICATION_FAILED", "CalDAV authentication was rejected.")).when(client).validateConnection();
        var result = provider(client, mock(CalDavEventMapper.class)).testConnection();
        assertEquals(CalendarConnectionStatus.UNAVAILABLE, result.status()); assertEquals("CalDAV authentication was rejected.", result.message());
    }
    @Test void timeoutDoesNotReturnStaleEvents() {
        var client = mock(CalDavClient.class); var start = OffsetDateTime.parse("2026-07-20T09:00:00-04:00");
        when(client.fetchCalendarData(any(java.net.URI.class), any(), any())).thenThrow(new CalDavException("TIMEOUT", "CalDAV request timed out."));
        assertEquals("TIMEOUT", assertThrows(CalDavException.class, () -> provider(client, mock(CalDavEventMapper.class)).fetchEvents(start, start.plusDays(1))).getCode());
    }
    @Test void malformedCalendarResponseFailsProviderSafely() {
        var client = mock(CalDavClient.class); var mapper = mock(CalDavEventMapper.class); var start = OffsetDateTime.parse("2026-07-20T09:00:00-04:00");
        when(client.fetchCalendarData(any(java.net.URI.class), any(), any())).thenReturn(List.of("bad")); when(mapper.map(any(), any(), any(), any())).thenThrow(new CalDavException("MALFORMED_ICALENDAR", "CalDAV returned malformed calendar data."));
        assertEquals("MALFORMED_ICALENDAR", assertThrows(CalDavException.class, () -> provider(client, mapper).fetchEvents(start, start.plusDays(1))).getCode());
    }
    @Test void missingCredentialEncryptionConfigurationIsAnExpectedState() {
        var configuration = new CalDavConfiguration(); configuration.setEnabled(true);
        var persistence = mock(CalDavPersistenceService.class); doThrow(new CredentialStoreException("Credential encryption is not configured.")).when(persistence).resolve(configuration);
        var client = mock(CalDavClient.class);
        var provider = new CalDavCalendarProvider(configuration, client, mock(CalDavEventMapper.class), mock(CalDavDiscoveryService.class), mock(com.oneil.wellness.walkplanner.calendar.persistence.ProviderConnectionService.class), persistence);
        var connection = provider.testConnection();
        assertEquals(CalendarConnectionStatus.CONFIGURATION_REQUIRED, connection.status());
        assertEquals("CalDAV credentials are unavailable because provider credential encryption is not configured.", connection.message());
        assertEquals("CONFIGURATION_REQUIRED", provider.discoverCalendars().status());
        verifyNoInteractions(client);
    }
    private CalDavCalendarProvider provider(CalDavClient client, CalDavEventMapper mapper) {
        var configuration = new CalDavConfiguration(); configuration.setEnabled(true); configuration.setServerUrl("http://localhost:5232/"); configuration.setUsername("test"); configuration.setPassword("test"); configuration.setCalendarPath("/test/calendar/");
        when(client.resolve(anyString())).thenReturn(java.net.URI.create("http://localhost:5232/test/calendar/"));
        var discovery = mock(CalDavDiscoveryService.class);
        when(discovery.discover()).thenReturn(new com.oneil.wellness.walkplanner.calendar.model.CalendarDiscoveryResult(
                List.of(new com.oneil.wellness.walkplanner.calendar.model.CalendarDescriptor(com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType.CALDAV,"cal_test","Test","/test/calendar/",true,true,true,true,null,null)),"EXPLICIT_PATH","Configured",java.time.Instant.now()));
        return new CalDavCalendarProvider(configuration, client, mapper, discovery);
    }
}
