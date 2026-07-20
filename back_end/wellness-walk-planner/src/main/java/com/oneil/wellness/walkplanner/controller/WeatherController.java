package com.oneil.wellness.walkplanner.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.exception.WeatherServiceException;
import com.oneil.wellness.walkplanner.recommendation.dto.PreferredTimeOfDay;
import com.oneil.wellness.walkplanner.recommendation.dto.CalendarRecommendationRequest;
import com.oneil.wellness.walkplanner.recommendation.dto.RainTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.recommendation.dto.TemperaturePreference;
import com.oneil.wellness.walkplanner.recommendation.dto.UnitSystem;
import com.oneil.wellness.walkplanner.recommendation.dto.WindTolerance;
import com.oneil.wellness.walkplanner.service.WeatherService;
import com.oneil.wellness.walkplanner.zip.service.ZipCodeNotFoundException;
import com.oneil.wellness.walkplanner.zip.service.ZipLookupException;
import com.oneil.wellness.walkplanner.zip.service.ZipWeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final String ZIP_CODE_PATTERN = "\\d{5}";

    private final WeatherService weatherService;
    private final ZipWeatherService zipWeatherService;

    public WeatherController(WeatherService weatherService, ZipWeatherService zipWeatherService) {
        this.weatherService = weatherService;
        this.zipWeatherService = zipWeatherService;
    }

    public WeatherResponse currentWeather(
            BigDecimal latitude,
            BigDecimal longitude) {
        return currentWeather(latitude, longitude, null, null, null, null, null, null, null);
    }

    @GetMapping("/current")
    public WeatherResponse currentWeather(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(required = false) Integer walkDurationMinutes,
            @RequestParam(required = false) String preferredTimeOfDay,
            @RequestParam(required = false) String temperaturePreference,
            @RequestParam(required = false) String rainTolerance,
            @RequestParam(required = false) String windTolerance,
            @RequestParam(required = false) Integer minimumScore,
            @RequestParam(required = false) String unitSystem) {
        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both latitude and longitude are required");
        }
        if (latitude.doubleValue() < -90 || latitude.doubleValue() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90");
        }
        if (longitude.doubleValue() < -180 || longitude.doubleValue() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180");
        }

        try {
            return weatherService.getCurrentWeather(latitude, longitude, preferences(
                    walkDurationMinutes,
                    preferredTimeOfDay,
                    temperaturePreference,
                    rainTolerance,
                    windTolerance,
                    minimumScore,
                    unitSystem));
        } catch (WeatherServiceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }

    public WeatherResponse currentWeatherByZip(String zipCode) {
        return currentWeatherByZip(zipCode, null, null, null, null, null, null, null);
    }

    @GetMapping("/current/{zipCode}")
    public WeatherResponse currentWeatherByZip(
            @PathVariable String zipCode,
            @RequestParam(required = false) Integer walkDurationMinutes,
            @RequestParam(required = false) String preferredTimeOfDay,
            @RequestParam(required = false) String temperaturePreference,
            @RequestParam(required = false) String rainTolerance,
            @RequestParam(required = false) String windTolerance,
            @RequestParam(required = false) Integer minimumScore,
            @RequestParam(required = false) String unitSystem) {
        if (zipCode == null || !zipCode.matches(ZIP_CODE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid 5-digit ZIP code");
        }

        try {
            return zipWeatherService.getCurrentWeather(zipCode, preferences(
                    walkDurationMinutes,
                    preferredTimeOfDay,
                    temperaturePreference,
                    rainTolerance,
                    windTolerance,
                    minimumScore,
                    unitSystem));
        } catch (ZipCodeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ZIP code not found");
        } catch (ZipLookupException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ZIP lookup service temporarily unavailable");
        } catch (WeatherServiceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather unavailable");
        }
    }

    @PostMapping("/current/{zipCode}/recommendation")
    public WeatherResponse currentWeatherByZipWithCalendar(
            @PathVariable String zipCode,
            @RequestBody(required = false) CalendarRecommendationRequest request) {
        if (zipCode == null || !zipCode.matches(ZIP_CODE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid 5-digit ZIP code");
        }
        CalendarRecommendationRequest normalizedRequest = request == null
                ? new CalendarRecommendationRequest(null, null)
                : request;
        try {
            return zipWeatherService.getCurrentWeather(
                    zipCode,
                    normalizedRequest.normalizedPreferences(),
                    normalizedRequest.normalizedEvents());
        } catch (ZipCodeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ZIP code not found");
        } catch (ZipLookupException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ZIP lookup service temporarily unavailable");
        } catch (WeatherServiceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather unavailable");
        }
    }

    private RecommendationPreferencesDto preferences(
            Integer walkDurationMinutes,
            String preferredTimeOfDay,
            String temperaturePreference,
            String rainTolerance,
            String windTolerance,
            Integer minimumScore,
            String unitSystem) {
        return new RecommendationPreferencesDto(
                walkDurationMinutes,
                parseEnum(PreferredTimeOfDay.class, preferredTimeOfDay),
                parseEnum(TemperaturePreference.class, temperaturePreference),
                parseEnum(RainTolerance.class, rainTolerance),
                parseEnum(WindTolerance.class, windTolerance),
                minimumScore,
                parseEnum(UnitSystem.class, unitSystem)).normalized();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
