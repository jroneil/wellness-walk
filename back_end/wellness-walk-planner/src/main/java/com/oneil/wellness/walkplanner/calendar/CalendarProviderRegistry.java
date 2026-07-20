package com.oneil.wellness.walkplanner.calendar;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType;

@Component
public class CalendarProviderRegistry {
    private final Map<CalendarProviderType, CalendarProvider> providers;

    public CalendarProviderRegistry(List<CalendarProvider> providers) {
        try {
            this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                    CalendarProvider::getType, Function.identity()));
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException("Duplicate calendar provider registration", ex);
        }
    }

    public Optional<CalendarProvider> find(CalendarProviderType type) { return Optional.ofNullable(providers.get(type)); }
    public CalendarProvider require(CalendarProviderType type) {
        return find(type).orElseThrow(() -> new NoSuchElementException("Calendar provider is not registered: " + type));
    }
    public List<CalendarProvider> list() { return List.copyOf(providers.values()); }
    public List<CalendarProvider> enabled() { return providers.values().stream().filter(CalendarProvider::isEnabled).toList(); }
}
