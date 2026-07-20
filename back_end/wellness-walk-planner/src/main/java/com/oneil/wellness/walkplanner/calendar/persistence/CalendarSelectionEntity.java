package com.oneil.wellness.walkplanner.calendar.persistence;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="calendar_selection",uniqueConstraints=@UniqueConstraint(name="uq_calendar_selection",columnNames={"provider_connection_id","provider_calendar_id"}))
public class CalendarSelectionEntity {
 @Id private UUID id; @Column(name="provider_connection_id",nullable=false) private UUID providerConnectionId; @Column(name="provider_calendar_id",nullable=false) private String providerCalendarId; @Column(name="provider_calendar_name",nullable=false) private String providerCalendarName; private boolean selected; @Column(name="read_only") private boolean readOnly; @Column(name="created_at") private Instant createdAt; @Column(name="updated_at") private Instant updatedAt;
 protected CalendarSelectionEntity(){} public CalendarSelectionEntity(UUID connectionId,String calendarId,String name,boolean selected,boolean readOnly){id=UUID.randomUUID();providerConnectionId=connectionId;providerCalendarId=calendarId;providerCalendarName=name;this.selected=selected;this.readOnly=readOnly;createdAt=updatedAt=Instant.now();}
 public String getProviderCalendarId(){return providerCalendarId;} public String getProviderCalendarName(){return providerCalendarName;} public boolean isSelected(){return selected;} public boolean isReadOnly(){return readOnly;} public void update(String name,boolean value,boolean readOnly){providerCalendarName=name;selected=value;this.readOnly=readOnly;updatedAt=Instant.now();}
}
