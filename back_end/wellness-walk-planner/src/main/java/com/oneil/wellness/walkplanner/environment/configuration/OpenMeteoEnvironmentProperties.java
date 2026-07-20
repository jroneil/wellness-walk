package com.oneil.wellness.walkplanner.environment.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "environment.open-meteo")
@Validated
public class OpenMeteoEnvironmentProperties {

    private String forecastBaseUrl = "https://api.open-meteo.com/v1/forecast";
    private String airQualityBaseUrl = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private String userAgent = "Wellness Window";
    private int connectionTimeoutMs = 3000;
    private int responseTimeoutMs = 5000;

    public String getForecastBaseUrl() {
        return forecastBaseUrl;
    }

    public void setForecastBaseUrl(String forecastBaseUrl) {
        this.forecastBaseUrl = forecastBaseUrl;
    }

    public String getAirQualityBaseUrl() {
        return airQualityBaseUrl;
    }

    public void setAirQualityBaseUrl(String airQualityBaseUrl) {
        this.airQualityBaseUrl = airQualityBaseUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getResponseTimeoutMs() {
        return responseTimeoutMs;
    }

    public void setResponseTimeoutMs(int responseTimeoutMs) {
        this.responseTimeoutMs = responseTimeoutMs;
    }
}
