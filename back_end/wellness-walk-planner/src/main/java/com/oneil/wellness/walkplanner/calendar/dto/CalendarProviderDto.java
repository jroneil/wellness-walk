package com.oneil.wellness.walkplanner.calendar.dto;

import com.oneil.wellness.walkplanner.calendar.CalendarProvider;
import com.oneil.wellness.walkplanner.calendar.model.*;

public record CalendarProviderDto(CalendarProviderType type, String displayName,
        boolean installationConfigured, boolean enabled, boolean connected, boolean authorizationRequired,
        int selectedCalendarCount, int discoveredCalendarCount,
        java.time.Instant lastSuccessfulSyncAt, java.time.Instant lastAttemptedSyncAt,
        CalendarConnectionStatus providerStatus, String safeMessage,
        CalendarConnectionStatus status, CalendarProviderCapabilities capabilities) {
    public static CalendarProviderDto from(CalendarProvider provider) {
        CalendarConnectionStatus status = provider.getConnectionStatus();
        CalendarProviderSyncStatus sync = provider.getSyncStatus();
        return new CalendarProviderDto(provider.getType(), provider.getDisplayName(),
                provider.isInstallationConfigured(), provider.isEnabled(), status == CalendarConnectionStatus.CONNECTED,
                status == CalendarConnectionStatus.AUTHORIZATION_REQUIRED,
                provider.getSelectedCalendarCount(), provider.getDiscoveredCalendarCount(),
                sync.lastSuccess(), sync.lastAttempt(), status, safeMessage(provider.getType(), status), status,
                provider.getCapabilities());
    }

    private static String safeMessage(CalendarProviderType type, CalendarConnectionStatus status) {
        if (type == CalendarProviderType.GOOGLE && status == CalendarConnectionStatus.DISABLED) {
            return "Google Calendar is not configured for this Wellness Window installation.";
        }
        return switch (status) {
            case DISABLED -> type + " is disabled.";
            case DISCONNECTED -> type == CalendarProviderType.GOOGLE
                    ? "Google Calendar is ready for account authorization."
                    : type + " is disconnected.";
            case CONFIGURATION_REQUIRED -> type + " requires server configuration.";
            case AUTHORIZATION_PENDING -> "Calendar authorization is in progress.";
            case AUTHORIZATION_REQUIRED -> "Calendar authorization is required.";
            case CONNECTED -> type + " is connected.";
            case UNAVAILABLE -> type + " is temporarily unavailable.";
        };
    }
}
