package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import com.oneil.wellness.walkplanner.calendar.credential.ProviderCredentialStore;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType;
import com.oneil.wellness.walkplanner.calendar.persistence.CalendarProviderConnection;
import com.oneil.wellness.walkplanner.calendar.persistence.ProviderConnectionService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalDavPersistenceService {
    private final ProviderConnectionService connections;
    private final ProviderCredentialStore credentials;

    public CalDavPersistenceService(ProviderConnectionService connections, ProviderCredentialStore credentials) {
        this.connections = connections;
        this.credentials = credentials;
    }

    @Transactional
    public void resolve(CalDavConfiguration config) {
        Optional<CalendarProviderConnection> existing = connections.find(CalendarProviderType.CALDAV);
        CalendarProviderConnection connection = existing.orElseGet(() ->
                connections.getOrCreate(CalendarProviderType.CALDAV, "CalDAV", config.isEnabled()));

        if (existing.isEmpty()) {
            connection.configure(config.isEnabled(), "CalDAV", config.getServerUrl(), config.getCalendarPath(), config.getDefaultTimezone());
        } else {
            if (blank(config.getServerUrl())) config.setServerUrl(connection.getServerUrl());
            if (blank(config.getCalendarPath())) config.setCalendarPath(connection.getCalendarPath());
            if (connection.getDefaultTimezone() != null) config.setDefaultTimezone(connection.getDefaultTimezone());
        }

        applyExplicitCredential(connection.getId(), "username", config.getUsername());
        applyExplicitCredential(connection.getId(), "password", config.getPassword());
        if (blank(config.getUsername())) credentials.readSecret(connection.getId(), "username").ifPresent(config::setUsername);
        if (blank(config.getPassword())) credentials.readSecret(connection.getId(), "password").ifPresent(config::setPassword);
    }

    private void applyExplicitCredential(java.util.UUID connectionId, String name, String value) {
        if (blank(value)) return;
        Optional<String> stored = credentials.readSecret(connectionId, name);
        if (stored.isEmpty()) credentials.saveSecret(connectionId, name, value);
        else if (!stored.get().equals(value)) credentials.rotateSecret(connectionId, name, value);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
