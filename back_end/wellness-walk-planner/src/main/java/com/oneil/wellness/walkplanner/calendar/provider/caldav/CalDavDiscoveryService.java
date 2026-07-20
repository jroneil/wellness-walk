package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import com.oneil.wellness.walkplanner.calendar.model.*;

@Component
public class CalDavDiscoveryService {
    private final CalDavClient client; private final CalDavConfiguration configuration;
    public CalDavDiscoveryService(CalDavClient client, CalDavConfiguration configuration) { this.client = client; this.configuration = configuration; }

    public CalendarDiscoveryResult discover() {
        if (configuration.hasExplicitCalendarPath()) {
            CalendarDescriptor descriptor = descriptor(configuration.getCalendarPath(), "Configured calendar", true, true, null, null);
            return new CalendarDiscoveryResult(List.of(descriptor), "EXPLICIT_PATH", "Using configured CalDAV calendar path.", Instant.now());
        }
        String principalXml = client.request(client.serverUri(), "PROPFIND", propfind("<d:current-user-principal/><d:principal-URL/>"), "0");
        String principalHref = firstHref(parse(principalXml), "current-user-principal", "principal-URL")
                .orElseThrow(() -> new CalDavException("PRINCIPAL_NOT_FOUND", "CalDAV current user principal was not found."));
        URI principal = client.resolve(principalHref);
        String homeXml = client.request(principal, "PROPFIND", propfind("<c:calendar-home-set/>"), "0");
        String homeHref = firstHref(parse(homeXml), "calendar-home-set")
                .orElseThrow(() -> new CalDavException("CALENDAR_HOME_NOT_FOUND", "CalDAV calendar home was not found."));
        URI home = client.resolve(homeHref);
        String calendarsXml = client.request(home, "PROPFIND", propfind("<d:displayname/><d:resourcetype/><c:supported-calendar-component-set/><d:current-user-privilege-set/><a:calendar-color/><c:calendar-description/>"), "1");
        List<CalendarDescriptor> calendars = enumerate(parse(calendarsXml));
        if (calendars.isEmpty()) return new CalendarDiscoveryResult(List.of(), "NO_CALENDARS", "No event calendars were discovered.", Instant.now());
        Set<String> configured = new HashSet<>(configuration.getCalendarIds());
        if (!configured.isEmpty()) calendars = calendars.stream().map(c -> c.withSelected(configured.contains(c.calendarId()))).toList();
        else if (calendars.stream().filter(CalendarDescriptor::supportsEvents).count() == 1) calendars = calendars.stream().map(c -> c.withSelected(c.supportsEvents())).toList();
        String status = calendars.stream().anyMatch(CalendarDescriptor::selected) ? "DISCOVERED" : "SELECTION_REQUIRED";
        return new CalendarDiscoveryResult(calendars, status, "Discovered " + calendars.size() + " CalDAV calendar(s).", Instant.now());
    }

    List<CalendarDescriptor> enumerate(Document document) {
        List<CalendarDescriptor> result = new ArrayList<>(); NodeList responses = document.getElementsByTagNameNS("DAV:", "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i); String href = childText(response, "DAV:", "href").orElse(null); if (href == null) continue;
            if (!hasDescendant(response, "urn:ietf:params:xml:ns:caldav", "calendar")) continue;
            boolean supportsEvents = componentNames(response).contains("VEVENT");
            String displayName = descendantText(response, "DAV:", "displayname").orElse("Calendar");
            boolean writable = privilegeNames(response).contains("write") || privilegeNames(response).contains("write-content");
            String color = descendantTextByLocalName(response, "calendar-color").orElse(null);
            String description = descendantText(response, "urn:ietf:params:xml:ns:caldav", "calendar-description").orElse(null);
            CalendarDescriptor value = descriptor(href, displayName, false, supportsEvents, color, description);
            result.add(new CalendarDescriptor(value.providerType(), value.calendarId(), value.displayName(), value.calendarUrl(),
                    value.enabled(), !writable, value.supportsEvents(), false, value.color(), value.description()));
        }
        return result;
    }

    private CalendarDescriptor descriptor(String href, String name, boolean selected, boolean supportsEvents, String color, String description) {
        return new CalendarDescriptor(CalendarProviderType.CALDAV, id(href), name, href, true, true, supportsEvents, selected, color, description);
    }
    private String id(String href) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(href.getBytes(StandardCharsets.UTF_8)); return "cal_" + HexFormat.of().formatHex(hash, 0, 8); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private String propfind(String properties) { return "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\" xmlns:a=\"http://apple.com/ns/ical/\"><d:prop>" + properties + "</d:prop></d:propfind>"; }
    Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); factory.setFeature("http://xml.org/sax/features/external-general-entities", false); factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            var builder=factory.newDocumentBuilder();builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){@Override public void error(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}@Override public void fatalError(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}});return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) { throw new CalDavException("MALFORMED_XML", "CalDAV discovery returned malformed XML.", ex); }
    }
    private Optional<String> firstHref(Document doc, String... parents) { for (String parent : parents) { NodeList nodes = doc.getElementsByTagNameNS("*", parent); for (int i=0;i<nodes.getLength();i++) { Optional<String> value=descendantText((Element)nodes.item(i),"DAV:","href"); if(value.isPresent()) return value; } } return Optional.empty(); }
    private Optional<String> childText(Element e,String ns,String local){ NodeList n=e.getElementsByTagNameNS(ns,local); return n.getLength()==0?Optional.empty():Optional.ofNullable(n.item(0).getTextContent()).map(String::trim).filter(s->!s.isEmpty()); }
    private Optional<String> descendantText(Element e,String ns,String local){ return childText(e,ns,local); }
    private Optional<String> descendantTextByLocalName(Element e,String local){ NodeList n=e.getElementsByTagNameNS("*",local); return n.getLength()==0?Optional.empty():Optional.ofNullable(n.item(0).getTextContent()).map(String::trim).filter(s->!s.isEmpty()); }
    private boolean hasDescendant(Element e,String ns,String local){ return e.getElementsByTagNameNS(ns,local).getLength()>0; }
    private Set<String> componentNames(Element e){ Set<String> r=new HashSet<>(); NodeList n=e.getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav","comp"); for(int i=0;i<n.getLength();i++){String v=((Element)n.item(i)).getAttribute("name");if(!v.isBlank())r.add(v.toUpperCase());} return r; }
    private Set<String> privilegeNames(Element e){ Set<String> r=new HashSet<>(); NodeList n=e.getElementsByTagNameNS("DAV:","privilege"); for(int i=0;i<n.getLength();i++){NodeList c=n.item(i).getChildNodes();for(int j=0;j<c.getLength();j++)if(c.item(j) instanceof Element el)r.add(el.getLocalName());}return r; }
}
