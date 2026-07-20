package com.oneil.wellness.walkplanner.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.exception.WeatherServiceException;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.weather.client.NwsWeatherClient;

@Service
public class WeatherService {

    private final NwsWeatherClient nwsWeatherClient;

    public WeatherService(NwsWeatherClient nwsWeatherClient) {
        this.nwsWeatherClient = nwsWeatherClient;
    }

    public WeatherResponse getCurrentWeather(BigDecimal latitude, BigDecimal longitude) {
        return getCurrentWeather(latitude, longitude, RecommendationPreferencesDto.defaults());
    }

    public WeatherResponse getCurrentWeather(BigDecimal latitude, BigDecimal longitude, RecommendationPreferencesDto preferences) {
        return getCurrentWeather(latitude, longitude, preferences, List.of());
    }

    public WeatherResponse getCurrentWeather(BigDecimal latitude, BigDecimal longitude,
            RecommendationPreferencesDto preferences, List<CalendarEvent> calendarEvents) {
        try {
            return nwsWeatherClient.fetchCurrentWeather(latitude, longitude,
                    preferences == null ? RecommendationPreferencesDto.defaults() : preferences.normalized(),
                    calendarEvents == null ? List.of() : calendarEvents);
        } catch (WeatherServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new WeatherServiceException("Weather provider error", ex);
        }
    }
}
