package com.oneil.wellness.walkplanner.calendar.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CalendarSelectionRepository extends JpaRepository<CalendarSelectionEntity,UUID>{List<CalendarSelectionEntity> findByProviderConnectionId(UUID id);Optional<CalendarSelectionEntity> findByProviderConnectionIdAndProviderCalendarId(UUID id,String calendarId);}
