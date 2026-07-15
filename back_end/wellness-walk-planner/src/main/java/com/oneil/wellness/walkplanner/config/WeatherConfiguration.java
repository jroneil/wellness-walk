package com.oneil.wellness.walkplanner.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WeatherNwsProperties.class, ZipLookupProperties.class})
public class WeatherConfiguration {
}
