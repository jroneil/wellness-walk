package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class CalDavDiscoveryServiceTest {
    @Test void discoversPrincipalHomeAndSingleCalendarWithAutoSelection() {
        var fixture=fixture(calendarResponse("/cal/test/work/","Work","VEVENT",false));
        var result=fixture.service.discover();
        assertEquals("DISCOVERED",result.status());assertEquals("Work",result.calendars().getFirst().displayName());assertTrue(result.calendars().getFirst().selected());assertTrue(result.calendars().getFirst().readOnly());
    }
    @Test void multipleCalendarsRequireExplicitSelection() {
        var fixture=fixture(calendarResponse("/cal/test/work/","Work","VEVENT",false)+calendarResponse("/cal/test/personal/","Personal","VEVENT",true));
        var result=fixture.service.discover();assertEquals("SELECTION_REQUIRED",result.status());assertEquals(2,result.calendars().size());assertTrue(result.calendars().stream().noneMatch(c->c.selected()));assertFalse(result.calendars().get(1).readOnly());
    }
    @Test void ignoresCollectionsWithoutEventSupport() {
        var fixture=fixture(calendarResponse("/cal/test/tasks/","Tasks","VTODO",false));
        var result=fixture.service.discover();assertEquals(1,result.calendars().size());assertFalse(result.calendars().getFirst().supportsEvents());assertEquals("SELECTION_REQUIRED",result.status());
    }
    @Test void zeroCalendarsReturnsStructuredState() { assertEquals("NO_CALENDARS",fixture("").service.discover().status()); }
    @Test void explicitPathOverridesDiscovery() {
        var config=new CalDavConfiguration();config.setCalendarPath("/explicit/");var service=new CalDavDiscoveryService(new StubClient(config),config);
        var result=service.discover();assertEquals("EXPLICIT_PATH",result.status());assertTrue(result.calendars().getFirst().selected());
    }
    @Test void malformedXmlIsSanitized() { assertEquals("MALFORMED_XML",assertThrows(CalDavException.class,()->fixture("<broken").service.discover()).getCode()); }

    private Fixture fixture(String responses) {
        var config=new CalDavConfiguration();config.setServerUrl("http://localhost:5232/");
        var client=new StubClient(config,
                multistatus("<d:response><d:href>/</d:href><d:propstat><d:prop><d:current-user-principal><d:href>/test/</d:href></d:current-user-principal></d:prop></d:propstat></d:response>"),
                multistatus("<d:response><d:href>/test/</d:href><d:propstat><d:prop><c:calendar-home-set><d:href>/cal/test/</d:href></c:calendar-home-set></d:prop></d:propstat></d:response>"),multistatus(responses));
        return new Fixture(new CalDavDiscoveryService(client,config));
    }
    private String calendarResponse(String href,String name,String component,boolean writable){return "<d:response><d:href>"+href+"</d:href><d:propstat><d:prop><d:displayname>"+name+"</d:displayname><d:resourcetype><d:collection/><c:calendar/></d:resourcetype><c:supported-calendar-component-set><c:comp name=\""+component+"\"/></c:supported-calendar-component-set><d:current-user-privilege-set><d:privilege><d:read/></d:privilege>"+(writable?"<d:privilege><d:write-content/></d:privilege>":"")+"</d:current-user-privilege-set></d:prop></d:propstat></d:response>";}
    private String multistatus(String body){return "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">"+body+"</d:multistatus>";}
    private record Fixture(CalDavDiscoveryService service){}
    private static final class StubClient extends CalDavClient {
        private final ArrayDeque<String> responses;
        StubClient(CalDavConfiguration config,String... responses){super(config);this.responses=new ArrayDeque<>(java.util.List.of(responses));}
        @Override String request(URI uri,String method,String body,String depth){return responses.removeFirst();}
    }
}
