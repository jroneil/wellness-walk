package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import java.io.StringReader;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;
import org.springframework.stereotype.Component;
import com.oneil.wellness.walkplanner.calendar.model.*;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.DateProperty;

@Component
public class CalDavEventMapper {
    private final CalDavConfiguration configuration;
    public CalDavEventMapper(CalDavConfiguration configuration) { this.configuration = configuration; }

    public List<CalendarEvent> map(String icalendar, OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {
        CalendarDescriptor descriptor = new CalendarDescriptor(CalendarProviderType.CALDAV, "caldav", "CalDAV", "", true, true, true, true, null, null);
        return map(icalendar, rangeStart, rangeEnd, descriptor);
    }

    public List<CalendarEvent> map(String icalendar, OffsetDateTime rangeStart, OffsetDateTime rangeEnd, CalendarDescriptor calendarDescriptor) {
        validateRange(rangeStart, rangeEnd);
        try {
            net.fortuna.ical4j.model.Calendar calendar = new CalendarBuilder().build(new StringReader(icalendar));
            List<VEvent> events = calendar.getComponents().stream().filter(VEvent.class::isInstance).map(VEvent.class::cast).toList();
            Map<String, List<VEvent>> byUid = new LinkedHashMap<>();
            for (VEvent event : events) byUid.computeIfAbsent(value(event, Property.UID).orElseThrow(() -> malformed("CalDAV event is missing UID.")), key -> new ArrayList<>()).add(event);
            List<CalendarEvent> result = new ArrayList<>();
            for (Map.Entry<String, List<VEvent>> entry : byUid.entrySet()) result.addAll(expand(entry.getKey(), entry.getValue(), rangeStart, rangeEnd, calendarDescriptor));
            if (result.size() > configuration.getMaximumEventsPerCalendar()) throw new CalDavException("EVENT_LIMIT_EXCEEDED", "CalDAV calendar produced too many event occurrences.");
            return result.stream().sorted(Comparator.comparing(CalendarEvent::startTime)).toList();
        } catch (CalDavException ex) { throw ex; }
        catch (Exception ex) { throw new CalDavException("MALFORMED_ICALENDAR", "CalDAV returned malformed calendar data.", ex); }
    }

    private List<CalendarEvent> expand(String uid, List<VEvent> components, OffsetDateTime rangeStart, OffsetDateTime rangeEnd, CalendarDescriptor descriptor) {
        VEvent master = components.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isEmpty()).findFirst().orElse(null);
        Map<Instant, VEvent> overrides = new HashMap<>();
        for (VEvent event : components) event.<DateProperty<?>>getProperty(Property.RECURRENCE_ID).ifPresent(property -> overrides.put(toInstant(property.getDate(), zone(event)), event));
        List<CalendarEvent> result = new ArrayList<>();
        if (master != null && !cancelled(master)) {
            for (Period<ZonedDateTime> period : periods(master, rangeStart, rangeEnd)) {
                Instant occurrence = period.getStart().toInstant(); VEvent override = overrides.remove(occurrence);
                if (override != null) { if (!cancelled(override)) result.add(toEvent(uid, override, occurrence, singlePeriod(override, rangeStart, rangeEnd), descriptor)); }
                else result.add(toEvent(uid, master, occurrence, period, descriptor));
                if (result.size() > configuration.getMaximumOccurrencesPerEvent()) throw new CalDavException("RECURRENCE_LIMIT_EXCEEDED", "CalDAV recurrence exceeded the configured occurrence limit.");
            }
        }
        for (Map.Entry<Instant, VEvent> override : overrides.entrySet()) if (!cancelled(override.getValue())) {
            Period<ZonedDateTime> period = singlePeriod(override.getValue(), rangeStart, rangeEnd);
            if (period != null && period.getStart().toInstant().isBefore(rangeEnd.toInstant()) && period.getEnd().toInstant().isAfter(rangeStart.toInstant())) result.add(toEvent(uid, override.getValue(), override.getKey(), period, descriptor));
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Period<ZonedDateTime>> periods(VEvent event, OffsetDateTime start, OffsetDateTime end) {
        ZoneId zone = zone(event); applyFloatingZone(event, zone);
        Period<ZonedDateTime> boundary = new Period<>(start.atZoneSameInstant(zone), end.atZoneSameInstant(zone));
        Set raw = event.calculateRecurrenceSet(boundary); List<Period<ZonedDateTime>> result = new ArrayList<>();
        for (Object value : raw) { Period<?> p=(Period<?>)value; result.add(new Period<>(toZoned(p.getStart(), zone), toZoned(p.getEnd(), zone))); }
        return result;
    }
    private Period<ZonedDateTime> singlePeriod(VEvent event, OffsetDateTime start, OffsetDateTime end) { List<Period<ZonedDateTime>> values=periods(event,start,end); return values.isEmpty()?null:values.getFirst(); }
    private CalendarEvent toEvent(String uid, VEvent component, Instant occurrenceId, Period<ZonedDateTime> period, CalendarDescriptor descriptor) {
        if (period == null) throw malformed("CalDAV override has no bounded occurrence.");
        ZoneId zone=zone(component); boolean allDay=component.<DateProperty<?>>getProperty(Property.DTSTART).map(p->p.getDate() instanceof LocalDate).orElse(false);
        boolean busy=!value(component,Property.TRANSP).map("TRANSPARENT"::equalsIgnoreCase).orElse(false);
        String title=value(component,Property.SUMMARY).filter(s->!s.isBlank()).orElse("Busy");
        OffsetDateTime start=period.getStart().toOffsetDateTime(), end=period.getEnd().toOffsetDateTime();
        String identity="caldav:"+descriptor.calendarId()+":"+uid+":"+occurrenceId;
        return new CalendarEvent(identity,title,start,end,busy,CalendarSource.CALDAV,CalendarProviderType.CALDAV,uid,
                descriptor.calendarId(),occurrenceId.toString(),zone.getId(),allDay);
    }
    private boolean cancelled(VEvent event){return value(event,Property.STATUS).map("CANCELLED"::equalsIgnoreCase).orElse(false);}
    private Optional<String> value(VEvent event,String name){return event.<Property>getProperty(name).map(Property::getValue);}
    private ZoneId zone(VEvent event){
        Optional<DateProperty<?>> start=event.getProperty(Property.DTSTART); if(start.isEmpty())return ZoneId.of(configuration.getDefaultTimezone());
        Optional<TzId> tzid=start.get().getParameter("TZID"); if(tzid.isPresent())try{return ZoneId.of(tzid.get().getValue());}catch(RuntimeException first){try{return tzid.get().toZoneId();}catch(RuntimeException ignored){throw new CalDavException("TIMEZONE_ERROR","CalDAV event timezone is invalid.");}}
        Temporal value=start.get().getDate(); if(value instanceof ZonedDateTime z)return z.getZone(); if(value instanceof OffsetDateTime o)return o.getOffset(); if(value instanceof Instant)return ZoneOffset.UTC;
        return ZoneId.of(configuration.getDefaultTimezone());
    }
    private void applyFloatingZone(VEvent event,ZoneId zone){event.<DateProperty<?>>getProperty(Property.DTSTART).ifPresent(p->p.setDefaultTimeZone(zone));event.<DateProperty<?>>getProperty(Property.DTEND).ifPresent(p->p.setDefaultTimeZone(zone));}
    private ZonedDateTime toZoned(Temporal value,ZoneId zone){if(value instanceof ZonedDateTime z)return z;if(value instanceof OffsetDateTime o)return o.toZonedDateTime();if(value instanceof Instant i)return i.atZone(zone);if(value instanceof LocalDateTime l)return l.atZone(zone);if(value instanceof LocalDate d)return d.atStartOfDay(zone);throw new CalDavException("TIMEZONE_ERROR","CalDAV event time type is unsupported.");}
    private Instant toInstant(Temporal value,ZoneId zone){return toZoned(value,zone).toInstant();}
    private void validateRange(OffsetDateTime start,OffsetDateTime end){if(start==null||end==null||!start.isBefore(end)||Duration.between(start,end).toDays()>configuration.getMaximumExpansionDays())throw new CalDavException("RANGE_LIMIT_EXCEEDED","Calendar synchronization range exceeds the configured limit.");}
    private CalDavException malformed(String message){return new CalDavException("MALFORMED_ICALENDAR",message);}
}
