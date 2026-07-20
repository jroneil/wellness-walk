package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.oneil.wellness.walkplanner.calendar.CalendarProvider;
import com.oneil.wellness.walkplanner.calendar.credential.EncryptedProviderCredentialStore.CredentialStoreException;
import com.oneil.wellness.walkplanner.calendar.model.*;

@Component
public class CalDavCalendarProvider implements CalendarProvider {
    private static final Logger log = LoggerFactory.getLogger(CalDavCalendarProvider.class);
    private final CalDavConfiguration configuration; private final CalDavClient client; private final CalDavEventMapper mapper; private final CalDavDiscoveryService discoveryService; private final com.oneil.wellness.walkplanner.calendar.persistence.ProviderConnectionService connections; private final CalDavPersistenceService persistence;
    private final AtomicReference<CalendarConnectionStatus> status = new AtomicReference<>(CalendarConnectionStatus.DISCONNECTED);
    private final AtomicReference<List<CalendarDescriptor>> calendars = new AtomicReference<>(List.of());
    private final AtomicReference<Set<String>> sessionSelection = new AtomicReference<>(Set.of());
    private final AtomicReference<CalendarProviderSyncStatus> syncStatus = new AtomicReference<>(CalendarProviderSyncStatus.never());

    @org.springframework.beans.factory.annotation.Autowired
    public CalDavCalendarProvider(CalDavConfiguration configuration, CalDavClient client, CalDavEventMapper mapper, CalDavDiscoveryService discoveryService, com.oneil.wellness.walkplanner.calendar.persistence.ProviderConnectionService connections, CalDavPersistenceService persistence) {
        this.configuration = configuration; this.client = client; this.mapper = mapper; this.discoveryService = discoveryService; this.connections=connections; this.persistence=persistence;
    }
    CalDavCalendarProvider(CalDavConfiguration configuration, CalDavClient client, CalDavEventMapper mapper, CalDavDiscoveryService discoveryService) { this.configuration=configuration;this.client=client;this.mapper=mapper;this.discoveryService=discoveryService;this.connections=null;this.persistence=null; }
    public CalendarProviderType getType() { return CalendarProviderType.CALDAV; }
    public String getDisplayName() { return "CalDAV"; }
    public boolean isEnabled() { return configuration.isEnabled() || (connections!=null && connections.find(getType()).map(com.oneil.wellness.walkplanner.calendar.persistence.CalendarProviderConnection::isEnabled).orElse(false)); }
    public CalendarConnectionStatus getConnectionStatus() { return !isEnabled() ? CalendarConnectionStatus.DISABLED : status.get(); }
    public CalendarConnectionResult testConnection() {
        if(!resolveConfiguration()) return new CalendarConnectionResult(CalendarConnectionStatus.CONFIGURATION_REQUIRED, credentialConfigurationMessage());
        if (!isEnabled()) return new CalendarConnectionResult(CalendarConnectionStatus.DISABLED, "CalDAV is disabled.");
        try { client.validateConnection(); status.set(CalendarConnectionStatus.CONNECTED); return new CalendarConnectionResult(status.get(), "CalDAV connection succeeded."); }
        catch (CalDavException ex) { status.set(CalendarConnectionStatus.UNAVAILABLE); return new CalendarConnectionResult(status.get(), ex.getMessage()); }
    }
    @Override public CalendarDiscoveryResult discoverCalendars() {
        if(!resolveConfiguration()) return new CalendarDiscoveryResult(List.of(), "CONFIGURATION_REQUIRED", credentialConfigurationMessage(), Instant.now());
        if (!isEnabled()) return new CalendarDiscoveryResult(List.of(), "DISABLED", "CalDAV is disabled.", Instant.now());
        long started = System.nanoTime();
        try {
            CalendarDiscoveryResult result = discoveryService.discover(); Set<String> durable=connections==null?Set.of():connections.selectedIds(getType()); if(sessionSelection.get().isEmpty()&&!durable.isEmpty())sessionSelection.set(durable); List<CalendarDescriptor> discovered = applySessionSelection(result.calendars()); calendars.set(discovered); status.set(CalendarConnectionStatus.CONNECTED);
            log.info("Calendar discovery provider={} calendars={} durationMs={}", getType(), discovered.size(), elapsed(started));
            return new CalendarDiscoveryResult(discovered, selectionStatus(discovered, result.status()), result.message(), result.discoveredAt());
        } catch (CalDavException ex) { status.set(CalendarConnectionStatus.UNAVAILABLE); log.warn("Calendar discovery provider={} category={} durationMs={}", getType(), ex.getCode(), elapsed(started)); throw ex; }
    }
    @Override public List<CalendarDescriptor> listCalendars() { return calendars.get().isEmpty() ? discoverCalendars().calendars() : calendars.get(); }
    @Override public List<CalendarDescriptor> selectCalendars(List<String> calendarIds) {
        Set<String> ids = Set.copyOf(calendarIds == null ? List.of() : calendarIds);
        List<CalendarDescriptor> available = listCalendars(); Set<String> known = available.stream().map(CalendarDescriptor::calendarId).collect(java.util.stream.Collectors.toSet());
        if (!known.containsAll(ids)) throw new CalDavException("UNKNOWN_CALENDAR", "One or more selected calendars are unavailable.");
        sessionSelection.set(ids); List<CalendarDescriptor> selected = available.stream().map(c -> c.withSelected(ids.contains(c.calendarId()))).toList(); calendars.set(selected); if(connections!=null)connections.persistSelections(getType(),selected,ids); return selected;
    }
    @Override public List<CalendarDescriptor> getSelectedCalendars() { return listCalendars().stream().filter(c -> c.selected() && c.supportsEvents()).toList(); }
    public List<CalendarEvent> fetchEvents(OffsetDateTime start, OffsetDateTime end) {
        if (!isEnabled()) return List.of(); List<CalendarDescriptor> selected = getSelectedCalendars();
        if (selected.isEmpty()) throw new CalDavException("CALENDAR_SELECTION_REQUIRED", "Select at least one CalDAV calendar.");
        return selected.stream().flatMap(calendar -> fetchEvents(calendar, start, end).stream()).toList();
    }
    @Override public List<CalendarEvent> fetchEvents(CalendarDescriptor calendar, OffsetDateTime start, OffsetDateTime end) {
        validateRange(start, end); long started = System.nanoTime(); Instant attempted=Instant.now();
        try {
            List<CalendarEvent> events = client.fetchCalendarData(client.resolve(calendar.calendarUrl()), start, end).stream()
                    .flatMap(value -> mapper.map(value, start, end, calendar).stream()).limit(configuration.getMaximumEventsPerCalendar()).toList();
            status.set(CalendarConnectionStatus.CONNECTED);syncStatus.set(new CalendarProviderSyncStatus(attempted,Instant.now(),"SUCCESS","Calendar synchronized.",List.of(new CalendarSynchronizationDetail(getType(),calendar.calendarId(),"SUCCESS",events.size(),null,"Calendar synchronized.")))); log.info("Calendar sync provider={} calendarId={} events={} durationMs={}", getType(), calendar.calendarId(), events.size(), elapsed(started)); return events;
        } catch (CalDavException ex) { status.set(CalendarConnectionStatus.UNAVAILABLE);syncStatus.set(new CalendarProviderSyncStatus(attempted,syncStatus.get().lastSuccess(),"FAILED",ex.getMessage(),List.of(new CalendarSynchronizationDetail(getType(),calendar.calendarId(),"FAILED",0,ex.getCode(),ex.getMessage())))); log.warn("Calendar sync provider={} calendarId={} category={} durationMs={}", getType(), calendar.calendarId(), ex.getCode(), elapsed(started)); throw ex; }
    }
    public CalendarProviderCapabilities getCapabilities() { return CalendarProviderCapabilities.readOnly(true, true); }
    @Override public boolean isInstallationConfigured(){return isEnabled()&&getConnectionStatus()!=CalendarConnectionStatus.CONFIGURATION_REQUIRED;}
    @Override public int getDiscoveredCalendarCount(){return calendars.get().size();}
    @Override public int getSelectedCalendarCount(){return calendars.get().isEmpty()?(connections==null?0:connections.selectedIds(getType()).size()):(int)calendars.get().stream().filter(CalendarDescriptor::selected).count();}
    public void disconnect() { status.set(CalendarConnectionStatus.DISCONNECTED); calendars.set(List.of()); sessionSelection.set(Set.of()); }
    @Override public CalendarProviderSyncStatus getSyncStatus(){return syncStatus.get();}
    private void validateRange(OffsetDateTime start, OffsetDateTime end) { if (start == null || end == null || !start.isBefore(end) || Duration.between(start, end).toDays() > configuration.getMaximumExpansionDays()) throw new CalDavException("RANGE_LIMIT_EXCEEDED", "Calendar synchronization range exceeds the configured limit."); }
    private List<CalendarDescriptor> applySessionSelection(List<CalendarDescriptor> input) { Set<String> ids=sessionSelection.get(); return ids.isEmpty()?input:input.stream().map(c->c.withSelected(ids.contains(c.calendarId()))).toList(); }
    private String selectionStatus(List<CalendarDescriptor> input,String fallback){return input.stream().anyMatch(CalendarDescriptor::selected)?"DISCOVERED":fallback;}
    private boolean resolveConfiguration(){try{if(persistence!=null)persistence.resolve(configuration);return true;}catch(CredentialStoreException ex){status.set(CalendarConnectionStatus.CONFIGURATION_REQUIRED);log.warn("CalDAV configuration requires a provider credential master key");return false;}}
    private String credentialConfigurationMessage(){return "CalDAV credentials are unavailable because provider credential encryption is not configured.";}
    private long elapsed(long started){return (System.nanoTime()-started)/1_000_000;}
}
