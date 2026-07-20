package com.oneil.wellness.walkplanner.recommendation.engine.calendar;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;

@Component
public class AvailabilityAnalyzer {
    public List<CandidateWindow> analyze(List<CandidateWindow> candidates, List<CalendarEvent> events) {
        List<BusyPeriod> busy = mergeBusyPeriods(events);
        return candidates.stream().map(candidate -> {
            CalendarEvent conflict = busy.stream()
                    .filter(period -> period.start().isBefore(candidate.endTime())
                            && period.end().isAfter(candidate.startTime()))
                    .map(BusyPeriod::representative).findFirst().orElse(null);
            return candidate.withAvailability(conflict == null, conflict);
        }).toList();
    }

    public List<BusyPeriod> mergeBusyPeriods(List<CalendarEvent> events) {
        List<CalendarEvent> sorted = (events == null ? List.<CalendarEvent>of() : events).stream()
                .filter(event -> event != null && event.busy() && event.startTime() != null && event.endTime() != null
                        && event.endTime().isAfter(event.startTime()))
                .sorted(Comparator.comparing(CalendarEvent::startTime)).toList();
        List<BusyPeriod> merged = new ArrayList<>();
        for (CalendarEvent event : sorted) {
            if (merged.isEmpty()) {
                merged.add(new BusyPeriod(event.startTime(), event.endTime(), event));
                continue;
            }
            BusyPeriod last = merged.get(merged.size() - 1);
            if (!event.startTime().isAfter(last.end())) {
                OffsetDateTime end = event.endTime().isAfter(last.end()) ? event.endTime() : last.end();
                merged.set(merged.size() - 1, new BusyPeriod(last.start(), end, last.representative()));
            } else {
                merged.add(new BusyPeriod(event.startTime(), event.endTime(), event));
            }
        }
        return merged;
    }

    public record BusyPeriod(OffsetDateTime start, OffsetDateTime end, CalendarEvent representative) {}
}
