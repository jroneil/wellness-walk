package com.oneil.wellness.walkplanner.zip.service;

import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.service.WeatherService;
import com.oneil.wellness.walkplanner.zip.client.ZipLookupClient;

@Service
public class ZipWeatherService {

    private final ZipLookupClient zipLookupClient;
    private final WeatherService weatherService;

    public ZipWeatherService(ZipLookupClient zipLookupClient, WeatherService weatherService) {
        this.zipLookupClient = zipLookupClient;
        this.weatherService = weatherService;
    }

    public WeatherResponse getCurrentWeather(String zipCode) {
        ZipCoordinates coordinates = zipLookupClient.lookup(zipCode);
        return weatherService.getCurrentWeather(coordinates.latitude(), coordinates.longitude());
    }
}
