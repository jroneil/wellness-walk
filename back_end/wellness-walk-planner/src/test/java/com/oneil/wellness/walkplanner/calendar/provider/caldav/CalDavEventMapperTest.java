package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class CalDavEventMapperTest {
    private final CalDavEventMapper mapper = new CalDavEventMapper(new CalDavConfiguration());
    private final OffsetDateTime rangeStart = OffsetDateTime.parse("2026-03-07T00:00:00-05:00");
    private final OffsetDateTime rangeEnd = OffsetDateTime.parse("2026-03-12T00:00:00-04:00");

    @Test void mapsUtcEventAndMinimumNecessaryFields() {
        var event = mapper.map(calendar("UID:one\nSUMMARY:Design Review\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z"), rangeStart, rangeEnd).getFirst();
        assertEquals("Design Review", event.title()); assertTrue(event.busy()); assertEquals("one", event.providerEventId()); assertEquals("Z", event.timezone());
    }
    @Test void transparentEventIsFreeAndCancelledEventIsIgnored() {
        var free = mapper.map(calendar("UID:free\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nTRANSP:TRANSPARENT"), rangeStart, rangeEnd);
        var cancelled = mapper.map(calendar("UID:gone\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nSTATUS:CANCELLED"), rangeStart, rangeEnd);
        assertFalse(free.getFirst().busy()); assertTrue(cancelled.isEmpty());
    }
    @Test void allDayEventUsesCalendarDateSemantics() {
        var event = mapper.map(calendar("UID:day\nDTSTART;VALUE=DATE:20260308\nDTEND;VALUE=DATE:20260309"), rangeStart, rangeEnd).getFirst();
        assertTrue(event.allDay()); assertEquals(Duration.ofDays(1), Duration.between(event.startTime(), event.endTime()));
    }
    @Test void timezoneAwareEventCrossesDstWithZonePreserved() {
        var event = mapper.map(calendar("UID:dst\nDTSTART;TZID=America/New_York:20260308T013000\nDTEND;TZID=America/New_York:20260308T033000"), rangeStart, rangeEnd).getFirst();
        assertEquals("America/New_York", event.timezone()); assertEquals(Duration.ofHours(1), Duration.between(event.startTime().toInstant(), event.endTime().toInstant()));
    }
    @Test void timezoneAwareEventCrossesFallBackWithZonePreserved() {
        var event = mapper.map(
                calendar("UID:fall-back\nDTSTART;TZID=America/New_York:20261101T013000\nDTEND;TZID=America/New_York:20261101T023000"),
                OffsetDateTime.parse("2026-11-01T00:00:00-04:00"),
                OffsetDateTime.parse("2026-11-02T00:00:00-05:00")).getFirst();
        assertEquals("America/New_York", event.timezone());
        assertEquals(Duration.ofHours(2), Duration.between(event.startTime().toInstant(), event.endTime().toInstant()));
    }
    @Test void recurringEventsAreBoundedAndExclusionsApplied() {
        String event = "UID:repeat\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nRRULE:FREQ=DAILY;COUNT=10\nEXDATE:20260309T150000Z";
        var values = mapper.map(calendar(event), rangeStart, rangeEnd);
        assertEquals(4, values.size()); assertTrue(values.stream().noneMatch(value -> value.startTime().toInstant().equals(Instant.parse("2026-03-09T15:00:00Z"))));
    }
    @Test void malformedDataFailsSafely() {
        assertEquals("MALFORMED_ICALENDAR", assertThrows(CalDavException.class, () -> mapper.map("not a calendar", rangeStart, rangeEnd)).getCode());
    }
    @Test void supportsByDayMonthlyAndYearlyRules() {
        assertEquals(2,mapper.map(calendar("UID:byday\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nRRULE:FREQ=WEEKLY;COUNT=3;BYDAY=SA,SU"),rangeStart,rangeEnd).size());
        var expandedConfig=new CalDavConfiguration();expandedConfig.setMaximumExpansionDays(366);var expanded=new CalDavEventMapper(expandedConfig);
        var monthEnd=OffsetDateTime.parse("2026-06-01T00:00:00Z");
        assertEquals(3,expanded.map(calendar("UID:month\nDTSTART:20260310T150000Z\nDTEND:20260310T153000Z\nRRULE:FREQ=MONTHLY;COUNT=3;BYMONTHDAY=10"),rangeStart,monthEnd).size());
        var yearEnd=OffsetDateTime.parse("2027-01-01T00:00:00Z");
        assertEquals(1,expanded.map(calendar("UID:year\nDTSTART:20260310T150000Z\nDTEND:20260310T153000Z\nRRULE:FREQ=YEARLY;COUNT=3;BYMONTH=3"),rangeStart,yearEnd).size());
    }
    @Test void supportsRdateAndDetachedChangedAndCancelledOccurrences() {
        String input="BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:override\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nRRULE:FREQ=DAILY;COUNT=3\nRDATE:20260311T150000Z\nEND:VEVENT\nBEGIN:VEVENT\nUID:override\nRECURRENCE-ID:20260308T150000Z\nDTSTART:20260308T170000Z\nDTEND:20260308T180000Z\nEND:VEVENT\nBEGIN:VEVENT\nUID:override\nRECURRENCE-ID:20260309T150000Z\nDTSTART:20260309T150000Z\nDTEND:20260309T153000Z\nSTATUS:CANCELLED\nEND:VEVENT\nEND:VCALENDAR";
        var events=mapper.map(input,rangeStart,OffsetDateTime.parse("2026-03-12T00:00:00Z"));assertEquals(3,events.size());assertTrue(events.stream().anyMatch(e->e.startTime().toInstant().equals(Instant.parse("2026-03-08T17:00:00Z"))&&java.time.Duration.between(e.startTime(),e.endTime()).toHours()==1));assertTrue(events.stream().noneMatch(e->e.startTime().toInstant().equals(Instant.parse("2026-03-09T15:00:00Z"))));
    }
    @Test void enforcesRecurrenceAndRangeSafetyLimits() {
        var config=new CalDavConfiguration();config.setMaximumOccurrencesPerEvent(2);var limited=new CalDavEventMapper(config);
        assertEquals("RECURRENCE_LIMIT_EXCEEDED",assertThrows(CalDavException.class,()->limited.map(calendar("UID:limit\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z\nRRULE:FREQ=DAILY;COUNT=10"),rangeStart,rangeEnd)).getCode());
        assertEquals("RANGE_LIMIT_EXCEEDED",assertThrows(CalDavException.class,()->mapper.map(calendar("UID:range\nDTSTART:20260307T150000Z\nDTEND:20260307T153000Z"),rangeStart,rangeStart.plusDays(40))).getCode());
    }
    private String calendar(String event) { return "BEGIN:VCALENDAR\nBEGIN:VEVENT\n" + event + "\nEND:VEVENT\nEND:VCALENDAR"; }
}
