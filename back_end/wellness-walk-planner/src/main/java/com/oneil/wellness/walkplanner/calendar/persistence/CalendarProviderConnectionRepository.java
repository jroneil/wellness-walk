package com.oneil.wellness.walkplanner.calendar.persistence;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CalendarProviderConnectionRepository extends JpaRepository<CalendarProviderConnection,UUID>{Optional<CalendarProviderConnection> findByProviderType(CalendarProviderType type);}
