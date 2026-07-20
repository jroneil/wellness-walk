package com.oneil.wellness.walkplanner.recommendation.engine.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;

@Component
public class TimeWindowUtilities {
    public List<CandidateWindow> generateCandidates(List<HourlyForecastPeriod> periods, int durationMinutes,
            int intervalMinutes, java.time.Instant now) {
        List<TimedPeriod> timed = periods.stream().map(this::timed).filter(java.util.Objects::nonNull)
                .filter(item -> item.period().walkingRecommendation() != null
                        && item.period().walkingRecommendation().score() != null)
                .sorted(Comparator.comparing(TimedPeriod::start)).toList();
        if (timed.isEmpty()) return List.of();
        OffsetDateTime cursor = timed.get(0).start();
        OffsetDateTime horizonEnd = timed.get(timed.size() - 1).start().plusHours(1);
        List<CandidateWindow> result = new ArrayList<>();
        while (!cursor.plusMinutes(durationMinutes).isAfter(horizonEnd)) {
            OffsetDateTime candidateStart = cursor;
            OffsetDateTime end = candidateStart.plusMinutes(durationMinutes);
            if (!candidateStart.toInstant().isBefore(now)) {
                List<TimedPeriod> covered = timed.stream()
                        .filter(item -> item.start().isBefore(end) && item.start().plusHours(1).isAfter(candidateStart))
                        .toList();
                long coveredMinutes = covered.stream()
                        .mapToLong(item -> overlapMinutes(candidateStart, end, item.start(), item.start().plusHours(1))).sum();
                if (coveredMinutes >= durationMinutes) {
                    TimedPeriod representative = covered.stream()
                            .filter(item -> !item.start().isAfter(candidateStart) && item.start().plusHours(1).isAfter(candidateStart))
                            .findFirst().orElse(covered.get(0));
                    int weather = covered.stream().mapToInt(item -> item.period().walkingRecommendation().score()).min().orElse(0);
                    result.add(new CandidateWindow(candidateStart, end, representative.period(), weather, true, null, 0, 0));
                }
            }
            cursor = cursor.plusMinutes(intervalMinutes);
        }
        return result;
    }

    private long overlapMinutes(OffsetDateTime aStart, OffsetDateTime aEnd, OffsetDateTime bStart, OffsetDateTime bEnd) {
        OffsetDateTime start = aStart.isAfter(bStart) ? aStart : bStart;
        OffsetDateTime end = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return end.isAfter(start) ? java.time.Duration.between(start, end).toMinutes() : 0;
    }

    private TimedPeriod timed(HourlyForecastPeriod period) {
        try { return new TimedPeriod(OffsetDateTime.parse(period.startTime()), period); }
        catch (DateTimeParseException | NullPointerException ex) { return null; }
    }

    private record TimedPeriod(OffsetDateTime start, HourlyForecastPeriod period) {}
}
