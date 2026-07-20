package com.oneil.wellness.walkplanner.calendar.dto;

import java.time.Instant;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType;

public record CalendarProviderErrorResponse(CalendarProviderType providerType, String code, String message,
        Instant timestamp) {
}
