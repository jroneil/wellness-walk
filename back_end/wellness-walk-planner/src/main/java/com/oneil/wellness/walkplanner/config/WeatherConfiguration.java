package com.oneil.wellness.walkplanner.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.oneil.wellness.walkplanner.environment.configuration.OpenMeteoEnvironmentProperties;

@Configuration
@EnableConfigurationProperties({WeatherNwsProperties.class, ZipLookupProperties.class, OpenMeteoEnvironmentProperties.class})
public class WeatherConfiguration {
}
