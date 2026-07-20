package com.oneil.wellness.walkplanner.calendar.persistence;

import com.oneil.wellness.walkplanner.calendar.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="calendar_provider_connection")
public class CalendarProviderConnection {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(name="provider_type",nullable=false,unique=true) private CalendarProviderType providerType;
    @Column(name="display_name",nullable=false) private String displayName;
    private boolean enabled;
    @Enumerated(EnumType.STRING) @Column(name="connection_status",nullable=false) private CalendarConnectionStatus connectionStatus;
    private boolean selected;
    @Column(name="configuration_version",nullable=false) private int configurationVersion;
    @Column(name="server_url") private String serverUrl;
    @Column(name="calendar_path") private String calendarPath;
    @Column(name="default_timezone") private String defaultTimezone;
    @Column(name="last_successful_sync_at") private Instant lastSuccessfulSyncAt;
    @Column(name="last_attempted_sync_at") private Instant lastAttemptedSyncAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected CalendarProviderConnection() {}
    public CalendarProviderConnection(CalendarProviderType type,String name,boolean enabled){id=UUID.randomUUID();providerType=type;displayName=name;this.enabled=enabled;connectionStatus=CalendarConnectionStatus.DISCONNECTED;configurationVersion=1;createdAt=updatedAt=Instant.now();}
    public UUID getId(){return id;} public CalendarProviderType getProviderType(){return providerType;} public String getDisplayName(){return displayName;} public boolean isEnabled(){return enabled;} public CalendarConnectionStatus getConnectionStatus(){return connectionStatus;} public String getServerUrl(){return serverUrl;} public String getCalendarPath(){return calendarPath;} public String getDefaultTimezone(){return defaultTimezone;} public Instant getLastSuccessfulSyncAt(){return lastSuccessfulSyncAt;} public Instant getLastAttemptedSyncAt(){return lastAttemptedSyncAt;}
    public void configure(boolean value,String name,String url,String path,String zone){enabled=value;displayName=name;serverUrl=url;calendarPath=path;defaultTimezone=zone;configurationVersion++;updatedAt=Instant.now();}
    public void status(CalendarConnectionStatus value){connectionStatus=value;updatedAt=Instant.now();}
    public void syncAttempt(boolean success){lastAttemptedSyncAt=Instant.now();if(success)lastSuccessfulSyncAt=lastAttemptedSyncAt;updatedAt=lastAttemptedSyncAt;}
}
