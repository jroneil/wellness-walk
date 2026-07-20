package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "calendar.caldav")
public class CalDavConfiguration {
    private boolean enabled;
    private String serverUrl;
    private String username;
    private String password;
    private String calendarPath;
    private List<String> calendarIds = new ArrayList<>();
    private String defaultTimezone = "UTC";
    private int lookaheadDays = 7;
    private int maximumOccurrencesPerEvent = 500;
    private int maximumEventsPerCalendar = 2000;
    private int maximumResponseBytes = 2_000_000;
    private int maximumExpansionDays = 31;
    private Duration connectionTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCalendarPath() { return calendarPath; }
    public void setCalendarPath(String calendarPath) { this.calendarPath = calendarPath; }
    public List<String> getCalendarIds() { return List.copyOf(calendarIds); }
    public void setCalendarIds(List<String> calendarIds) { this.calendarIds = calendarIds == null ? new ArrayList<>() : new ArrayList<>(calendarIds); }
    public String getDefaultTimezone() { return defaultTimezone; }
    public void setDefaultTimezone(String defaultTimezone) { ZoneId.of(defaultTimezone); this.defaultTimezone = defaultTimezone; }
    public int getLookaheadDays() { return lookaheadDays; }
    public void setLookaheadDays(int lookaheadDays) { this.lookaheadDays = Math.max(1, Math.min(31, lookaheadDays)); }
    public int getMaximumOccurrencesPerEvent() { return maximumOccurrencesPerEvent; }
    public void setMaximumOccurrencesPerEvent(int value) { maximumOccurrencesPerEvent = Math.max(1, Math.min(5000, value)); }
    public int getMaximumEventsPerCalendar() { return maximumEventsPerCalendar; }
    public void setMaximumEventsPerCalendar(int value) { maximumEventsPerCalendar = Math.max(1, Math.min(10000, value)); }
    public int getMaximumResponseBytes() { return maximumResponseBytes; }
    public void setMaximumResponseBytes(int value) { maximumResponseBytes = Math.max(1024, Math.min(20_000_000, value)); }
    public int getMaximumExpansionDays() { return maximumExpansionDays; }
    public void setMaximumExpansionDays(int value) { maximumExpansionDays = Math.max(1, Math.min(366, value)); }
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Duration connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public boolean isComplete() { return notBlank(serverUrl) && notBlank(username) && notBlank(password); }
    public boolean hasExplicitCalendarPath() { return notBlank(calendarPath); }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
}
