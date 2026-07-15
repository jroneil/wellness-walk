package com.oneil.wellness.walkplanner.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.exception.WeatherServiceException;
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

    @GetMapping("/current")
    public WeatherResponse currentWeather(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
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
            return weatherService.getCurrentWeather(latitude, longitude);
        } catch (WeatherServiceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }

    @GetMapping("/current/{zipCode}")
    public WeatherResponse currentWeatherByZip(@PathVariable String zipCode) {
        if (zipCode == null || !zipCode.matches(ZIP_CODE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid 5-digit ZIP code");
        }

        try {
            return zipWeatherService.getCurrentWeather(zipCode);
        } catch (ZipCodeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ZIP code not found");
        } catch (ZipLookupException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ZIP lookup service temporarily unavailable");
        } catch (WeatherServiceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather unavailable");
        }
    }
}
