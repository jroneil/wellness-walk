package com.oneil.wellness.walkplanner.calendar.provider.manual;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import com.oneil.wellness.walkplanner.calendar.CalendarProvider;
import com.oneil.wellness.walkplanner.calendar.model.*;

@Component
public class ManualCalendarProvider implements CalendarProvider {
    public CalendarProviderType getType() { return CalendarProviderType.MANUAL; }
    public String getDisplayName() { return "Manual Calendar"; }
    public boolean isEnabled() { return true; }
    public CalendarConnectionStatus getConnectionStatus() { return CalendarConnectionStatus.CONNECTED; }
    public CalendarConnectionResult testConnection() { return new CalendarConnectionResult(getConnectionStatus(), "Browser-local manual calendar is available."); }
    public List<CalendarEvent> fetchEvents(OffsetDateTime start, OffsetDateTime end) { return List.of(); }
    public CalendarProviderCapabilities getCapabilities() { return new CalendarProviderCapabilities(true, true, false, false); }
}
