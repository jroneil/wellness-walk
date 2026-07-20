package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

@Component
public class CalDavClient {
    private static final Pattern CALENDAR_DATA = Pattern.compile("<(?:[^:>]+:)?calendar-data[^>]*>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</(?:[^:>]+:)?calendar-data>", Pattern.DOTALL);
    private static final DateTimeFormatter CALDAV_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);
    private final CalDavConfiguration configuration;
    public CalDavClient(CalDavConfiguration configuration) { this.configuration = configuration; }

    public void validateConnection() {
        request(serverUri(), "PROPFIND", "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:current-user-principal/></d:prop></d:propfind>", "0");
    }

    public List<String> fetchCalendarData(URI calendarUri, OffsetDateTime start, OffsetDateTime end) {
        String body = "<?xml version=\"1.0\"?><c:calendar-query xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:prop><c:calendar-data/></d:prop><c:filter><c:comp-filter name=\"VCALENDAR\"><c:comp-filter name=\"VEVENT\"><c:time-range start=\""
                + CALDAV_UTC.format(start.toInstant()) + "\" end=\"" + CALDAV_UTC.format(end.toInstant())
                + "\"/></c:comp-filter></c:comp-filter></c:filter></c:calendar-query>";
        String response = request(calendarUri, "REPORT", body, "1");
        Matcher matcher = CALENDAR_DATA.matcher(response); List<String> values = new ArrayList<>();
        while (matcher.find()) {
            if (values.size() >= configuration.getMaximumEventsPerCalendar()) throw new CalDavException("EVENT_LIMIT_EXCEEDED", "CalDAV calendar returned too many event resources.");
            values.add(unescapeXml(matcher.group(1).trim()));
        }
        return values;
    }

    public List<String> fetchCalendarData(OffsetDateTime start, OffsetDateTime end) {
        if (!configuration.hasExplicitCalendarPath()) throw new CalDavException("CALENDAR_SELECTION_REQUIRED", "A CalDAV calendar must be selected.");
        return fetchCalendarData(resolve(configuration.getCalendarPath()), start, end);
    }

    String request(URI uri, String method, String body, String depth) {
        if (!configuration.isComplete()) throw new CalDavException("NOT_CONFIGURED", "CalDAV configuration is incomplete.");
        validateUri(uri);
        try {
            String credentials = Base64.getEncoder().encodeToString((configuration.getUsername() + ":" + configuration.getPassword()).getBytes(StandardCharsets.UTF_8));
            HttpClient client = HttpClient.newBuilder().connectTimeout(configuration.getConnectionTimeout()).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(configuration.getReadTimeout())
                    .header("Authorization", "Basic " + credentials).header("Depth", depth)
                    .header("Content-Type", "application/xml; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) throw new CalDavException("AUTHENTICATION_FAILED", "CalDAV authentication was rejected.");
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new CalDavException("SERVER_ERROR", "CalDAV returned HTTP " + response.statusCode() + ".");
            validateResponseSize(response.body());
            return response.body();
        } catch (CalDavException ex) { throw ex; }
        catch (HttpTimeoutException ex) { throw new CalDavException("TIMEOUT", "CalDAV request timed out.", ex); }
        catch (Exception ex) { throw new CalDavException("UNAVAILABLE", "CalDAV provider could not be reached.", ex); }
    }

    void validateResponseSize(String responseBody) {
        if (responseBody.getBytes(StandardCharsets.UTF_8).length > configuration.getMaximumResponseBytes()) {
            throw new CalDavException("RESPONSE_TOO_LARGE", "CalDAV response exceeded the configured safety limit.");
        }
    }

    URI serverUri() { try { return URI.create(configuration.getServerUrl()); } catch (RuntimeException ex) { throw new CalDavException("INVALID_URL", "CalDAV server URL is invalid.", ex); } }
    URI resolve(String href) { return serverUri().resolve(href); }
    private void validateUri(URI uri) {
        if (uri.getUserInfo() != null) throw new CalDavException("INVALID_URL", "Credentials must not be embedded in the CalDAV URL.");
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !isLocal(uri)) throw new CalDavException("INSECURE_URL", "CalDAV requires HTTPS except for local development servers.");
    }
    private boolean isLocal(URI uri) { return "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "radicale".equalsIgnoreCase(uri.getHost()); }
    private String unescapeXml(String value) { return value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'"); }
}
