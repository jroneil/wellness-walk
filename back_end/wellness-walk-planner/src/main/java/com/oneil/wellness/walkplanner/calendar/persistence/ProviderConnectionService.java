package com.oneil.wellness.walkplanner.calendar.persistence;
import com.oneil.wellness.walkplanner.calendar.model.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service
public class ProviderConnectionService {
 private final CalendarProviderConnectionRepository connections; private final CalendarSelectionRepository selections;
 public ProviderConnectionService(CalendarProviderConnectionRepository c,CalendarSelectionRepository s){connections=c;selections=s;}
 @Transactional public CalendarProviderConnection getOrCreate(CalendarProviderType type,String name,boolean enabled){return connections.findByProviderType(type).orElseGet(()->connections.save(new CalendarProviderConnection(type,name,enabled)));}
 public Optional<CalendarProviderConnection> find(CalendarProviderType type){return connections.findByProviderType(type);}
 @Transactional public void persistSelections(CalendarProviderType type,List<CalendarDescriptor> calendars,Set<String> selected){CalendarProviderConnection c=getOrCreate(type,type.name(),true);for(CalendarDescriptor d:calendars){CalendarSelectionEntity e=selections.findByProviderConnectionIdAndProviderCalendarId(c.getId(),d.calendarId()).orElseGet(()->new CalendarSelectionEntity(c.getId(),d.calendarId(),d.displayName(),false,d.readOnly()));e.update(d.displayName(),selected.contains(d.calendarId()),d.readOnly());selections.save(e);}}
 public Set<String> selectedIds(CalendarProviderType type){return find(type).stream().flatMap(c->selections.findByProviderConnectionId(c.getId()).stream()).filter(CalendarSelectionEntity::isSelected).map(CalendarSelectionEntity::getProviderCalendarId).collect(java.util.stream.Collectors.toUnmodifiableSet());}
 @Transactional public void updateStatus(CalendarProviderType type,CalendarConnectionStatus status,boolean syncSuccess){find(type).ifPresent(c->{c.status(status);if(syncSuccess||status==CalendarConnectionStatus.UNAVAILABLE)c.syncAttempt(syncSuccess);});}
}
