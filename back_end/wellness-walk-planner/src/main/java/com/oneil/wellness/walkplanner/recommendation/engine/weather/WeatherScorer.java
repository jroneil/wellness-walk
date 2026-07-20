package com.oneil.wellness.walkplanner.recommendation.engine.weather;

import org.springframework.stereotype.Component;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.service.WalkingScoreService;

@Component
public class WeatherScorer {
    private final WalkingScoreService scoreService;
    public WeatherScorer(WalkingScoreService scoreService) { this.scoreService = scoreService; }
    public HourlyForecastPeriod score(HourlyForecastPeriod period) {
        return period.walkingRecommendation() == null
                ? period.withWalkingRecommendation(new com.oneil.wellness.walkplanner.recommendation.service.WalkingRecommendationService(scoreService).recommendationFor(period))
                : period;
    }
}
