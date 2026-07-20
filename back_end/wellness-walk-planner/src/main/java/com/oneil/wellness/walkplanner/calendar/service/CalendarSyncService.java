package com.oneil.wellness.walkplanner.calendar.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import com.oneil.wellness.walkplanner.calendar.*;
import com.oneil.wellness.walkplanner.calendar.model.*;
import com.oneil.wellness.walkplanner.calendar.provider.caldav.CalDavException;
import com.oneil.wellness.walkplanner.calendar.provider.google.GoogleProviderException;

@Service
public class CalendarSyncService {
    private final CalendarProviderRegistry registry;
    public CalendarSyncService(CalendarProviderRegistry registry) { this.registry = registry; }

    public CalendarSyncResult synchronize(OffsetDateTime start, OffsetDateTime end, List<CalendarEvent> manualEvents) {
        if (start == null || end == null || !start.isBefore(end)) throw new IllegalArgumentException("A bounded calendar range is required.");
        List<CalendarEvent> events = new ArrayList<>(manualEvents == null ? List.of() : manualEvents);
        List<CalendarSyncError> errors = new ArrayList<>();
        List<CalendarSynchronizationDetail> details = new ArrayList<>();
        for (CalendarProvider provider : registry.enabled()) {
            if (provider.getType() == CalendarProviderType.MANUAL) continue;
            try {
                List<CalendarDescriptor> selected = provider.getSelectedCalendars();
                if (selected.isEmpty()) {
                    List<CalendarEvent> fetched=provider.fetchEvents(start,end); events.addAll(fetched);
                    details.add(new CalendarSynchronizationDetail(provider.getType(),null,"SUCCESS",fetched.size(),null,"Provider synchronized."));
                }
                else for (CalendarDescriptor calendar : selected) {
                    try { List<CalendarEvent> fetched=provider.fetchEvents(calendar,start,end);events.addAll(fetched);details.add(new CalendarSynchronizationDetail(provider.getType(),calendar.calendarId(),"SUCCESS",fetched.size(),null,"Calendar synchronized.")); }
                    catch (CalDavException ex) { errors.add(new CalendarSyncError(provider.getType(),ex.getCode(),calendar.calendarId()+": "+ex.getMessage()));details.add(new CalendarSynchronizationDetail(provider.getType(),calendar.calendarId(),"FAILED",0,ex.getCode(),ex.getMessage())); }
                    catch (GoogleProviderException ex) { errors.add(new CalendarSyncError(provider.getType(),ex.getCode(),calendar.calendarId()+": "+ex.getMessage()));details.add(new CalendarSynchronizationDetail(provider.getType(),calendar.calendarId(),"FAILED",0,ex.getCode(),ex.getMessage())); }
                }
            }
            catch (CalDavException ex) { errors.add(new CalendarSyncError(provider.getType(), ex.getCode(), ex.getMessage())); }
            catch (GoogleProviderException ex) { errors.add(new CalendarSyncError(provider.getType(), ex.getCode(), ex.getMessage())); }
            catch (RuntimeException ex) { errors.add(new CalendarSyncError(provider.getType(), "PROVIDER_FAILURE", "Calendar provider is unavailable.")); }
        }
        Map<String, CalendarEvent> unique = new LinkedHashMap<>();
        for (CalendarEvent event : events) unique.putIfAbsent(identity(event), event);
        return new CalendarSyncResult(List.copyOf(unique.values()), errors, details, Instant.now());
    }
    private String identity(CalendarEvent event) {
        return event.providerType() + "|" + Optional.ofNullable(event.calendarId()).orElse("default") + "|"
                + Optional.ofNullable(event.providerEventId()).orElse(event.id()) + "|"
                + Optional.ofNullable(event.occurrenceId()).orElse(event.startTime().toInstant().toString());
    }
}
