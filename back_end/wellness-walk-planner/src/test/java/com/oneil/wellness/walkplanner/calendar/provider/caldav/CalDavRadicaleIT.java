package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalDavRadicaleIT {
    @Test void discoversSelectsAndReadsLiveRadicaleCalendars() {
        CalDavConfiguration config=config("wellness-dev-only");CalDavClient client=new CalDavClient(config);CalDavDiscoveryService discovery=new CalDavDiscoveryService(client,config);
        var result=discovery.discover();assertEquals("SELECTION_REQUIRED",result.status());assertEquals(2,result.calendars().size());assertTrue(result.calendars().stream().allMatch(c->c.supportsEvents()&&c.readOnly()==false));
        var mapper=new CalDavEventMapper(config);var start=OffsetDateTime.parse("2026-07-20T00:00:00-04:00");var end=start.plusDays(7);
        var events=result.calendars().stream().flatMap(calendar->client.fetchCalendarData(client.resolve(calendar.calendarUrl()),start,end).stream().flatMap(raw->mapper.map(raw,start,end,calendar).stream())).toList();
        assertTrue(events.stream().anyMatch(e->e.providerEventId().equals("radicale-all-day")&&Boolean.TRUE.equals(e.allDay())));
        assertTrue(events.stream().anyMatch(e->e.providerEventId().equals("radicale-transparent")&&!e.busy()));
        assertTrue(events.stream().noneMatch(e->e.providerEventId().equals("radicale-cancelled")));
        assertTrue(events.stream().anyMatch(e->e.title().equals("Moved planning")&&e.startTime().getHour()==10));
        assertTrue(events.stream().noneMatch(e->e.providerEventId().equals("radicale-recurring")&&e.startTime().getDayOfMonth()==21));
    }
    @Test void liveAuthenticationFailureIsSanitized() {
        var client=new CalDavClient(config("wrong-password"));var error=assertThrows(CalDavException.class,client::validateConnection);assertEquals("AUTHENTICATION_FAILED",error.getCode());assertFalse(error.getMessage().contains("wrong-password"));
    }
    private CalDavConfiguration config(String password){var value=new CalDavConfiguration();value.setEnabled(true);value.setServerUrl(System.getenv().getOrDefault("CALDAV_IT_SERVER_URL","http://localhost:5232/"));value.setUsername("wellness");value.setPassword(password);value.setDefaultTimezone("America/New_York");value.setMaximumExpansionDays(31);return value;}
}
