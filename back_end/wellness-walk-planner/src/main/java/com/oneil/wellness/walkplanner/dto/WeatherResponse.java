package com.oneil.wellness.walkplanner.dto;

import java.math.BigDecimal;
import java.util.List;

import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalConditionsDto;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.DailyOutlookDto;

public record WeatherResponse(
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        CurrentWeatherSummary current,
        EnvironmentalConditionsDto environmentalConditions,
        BestWalkingWindowDto bestWalkingWindow,
        List<HourlyForecastPeriod> hourlyForecast,
        List<DailyOutlookDto> weeklyOutlook) {
}
