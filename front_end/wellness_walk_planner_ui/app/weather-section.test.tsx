// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WeatherSection from "./weather-section";
import { getCurrentWeather } from "./weather-actions";
import { settingsStorageKey } from "./settings/settings";

vi.mock("./weather-actions", () => ({
  getCurrentWeather: vi.fn(),
}));

describe("WeatherSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it("renders polished current conditions and readable hourly forecast data", async () => {
    vi.mocked(getCurrentWeather).mockResolvedValue({
      weather: {
        locationName: "Washington, DC",
        latitude: 38.9072,
        longitude: -77.0369,
        current: {
          temperature: 72.4,
          temperatureUnit: "°F",
          feelsLike: 74,
          humidity: null,
          windSpeed: 12,
          windDirection: "W",
          weatherCondition: "Mostly Sunny",
          iconUrl: "https://api.weather.gov/icons/land/day/few?size=medium",
          observationTime: "2024-01-01T12:00:00Z",
          dataType: "HOURLY_FORECAST",
        },
        environmentalConditions: {
          actualTemperature: 74,
          feelsLikeTemperature: 97,
          temperatureUnit: "°F",
          feelsLikeMethod: "HEAT_INDEX",
          aqi: 42,
          aqiCategory: "Good",
          aqiObservationTime: "2024-01-01T13:00:00Z",
          aqiSource: "Open-Meteo Air Quality API",
          uvIndex: 8,
          uvCategory: "Very High",
          uvObservationOrForecastTime: "2024-01-01T13:00:00Z",
          uvSource: "Open-Meteo Forecast API",
          sunrise: "2024-01-01T07:15:00Z",
          sunset: "2024-01-01T17:10:00Z",
          daylightStatus: "DAYLIGHT",
          remainingDaylightMinutes: 250,
        },
        bestWalkingWindow: {
          startTime: "2024-01-01T13:00:00Z",
          endTime: "2024-01-01T14:00:00Z",
          score: 86,
          rating: "GREAT",
          ratingLabel: "Great",
          summary: "Great weather for a restorative walk.",
          positiveReasons: ["Comfortable feels-like temperature", "Some chance of rain", "Light wind"],
          warnings: ["High UV", "Sun protection recommended"],
          durationMinutes: 30,
          preferenceReasons: ["30-minute walk window", "Fits your rain tolerance"],
          minimumScore: 60,
          belowMinimumScore: false,
          minimumScoreMessage: null,
          availability: "AVAILABLE",
          selectionReason: "Highest available weather score.",
          conflictingEvent: null,
          idealWeatherWindow: null,
          weatherScore: 86,
          availabilityScore: 100,
          preferenceScore: 100,
          overallScore: 90,
          calendarReasons: ["Selected window is fully available"],
          noAvailableReason: null,
        },
        hourlyForecast: [
          {
            startTime: "2024-01-01T13:00:00Z",
            temperature: 74,
            actualTemperature: 74,
            temperatureUnit: "°F",
            shortForecast: "Sunny",
            iconUrl: "https://api.weather.gov/icons/land/day/skc?size=medium",
            precipitationProbability: 20,
            humidity: 45,
            windSpeed: 10,
            windDirection: "W",
            isDaytime: true,
            feelsLikeTemperature: 74,
            feelsLikeMethod: "ACTUAL_TEMPERATURE",
            uvIndex: 8,
            uvCategory: "Very High",
            uvObservationOrForecastTime: "2024-01-01T13:00:00Z",
            uvSource: "Open-Meteo Forecast API",
            aqi: 42,
            aqiCategory: "Good",
            aqiObservationTime: "2024-01-01T13:00:00Z",
            aqiSource: "Open-Meteo Air Quality API",
            sunrise: "2024-01-01T07:15:00Z",
            sunset: "2024-01-01T17:10:00Z",
            daylightStatus: "DAYLIGHT",
            remainingDaylightMinutes: 250,
            walkingRecommendation: {
              startTime: "2024-01-01T13:00:00Z",
              score: 86,
              rating: "GREAT",
              ratingLabel: "Great",
              recommended: true,
              temperatureScore: 30,
              precipitationScore: 16,
              windScore: 10,
              humidityScore: 10,
              daylightScore: 10,
              airQualityScore: 10,
              uvScore: 0,
              feelsLikeTemperature: 74,
              feelsLikeMethod: "ACTUAL_TEMPERATURE",
              reasons: ["Comfortable feels-like temperature", "Some chance of rain", "Light wind", "Good air quality", "Very High UV exposure"],
              warnings: ["High UV", "Sun protection recommended"],
            },
          },
          {
            startTime: "2024-01-01T14:00:00Z",
            temperature: 71,
            actualTemperature: 71,
            temperatureUnit: "°F",
            shortForecast: "Cloudy",
            iconUrl: null,
            precipitationProbability: null,
            humidity: null,
            windSpeed: 8,
            windDirection: "NW",
            isDaytime: true,
            feelsLikeTemperature: null,
            feelsLikeMethod: null,
            uvIndex: null,
            uvCategory: "Unavailable",
            uvObservationOrForecastTime: null,
            uvSource: null,
            aqi: null,
            aqiCategory: "Unavailable",
            aqiObservationTime: null,
            aqiSource: null,
            sunrise: null,
            sunset: null,
            daylightStatus: "UNKNOWN",
            remainingDaylightMinutes: null,
            walkingRecommendation: {
              startTime: "2024-01-01T14:00:00Z",
              score: null,
              rating: null,
              ratingLabel: "Not enough data",
              recommended: false,
              temperatureScore: null,
              precipitationScore: null,
              windScore: null,
              humidityScore: null,
              daylightScore: null,
              airQualityScore: null,
              uvScore: null,
              feelsLikeTemperature: null,
              feelsLikeMethod: null,
              reasons: ["Temperature unavailable"],
              warnings: [],
            },
          },
        ],
        weeklyOutlook: [
          {
            date: "2024-01-01",
            dayName: "Monday",
            iconUrl: "https://api.weather.gov/icons/land/day/skc?size=medium",
            shortForecast: "Sunny",
            highTemperature: 74,
            lowTemperature: 62,
            temperatureUnit: "°F",
            precipitationProbability: 10,
            representativeScore: 86,
            rating: "GREAT",
            ratingLabel: "Great",
            bestAvailableTime: "2024-01-01T13:00:00Z",
            summary: "Great walking weather",
            environmentalWarnings: ["High UV", "Very Hot"],
          },
          {
            date: "2024-01-02",
            dayName: "Tuesday",
            iconUrl: null,
            shortForecast: "Rain",
            highTemperature: 70,
            lowTemperature: 58,
            temperatureUnit: "°F",
            precipitationProbability: null,
            representativeScore: null,
            rating: null,
            ratingLabel: "Not enough hourly data",
            bestAvailableTime: null,
            summary: "Not enough hourly data",
            environmentalWarnings: [],
          },
        ],
      },
      error: null,
    });

    render(<WeatherSection />);

    fireEvent.change(screen.getByLabelText(/zip code/i), {
      target: { value: "02108" },
    });
    fireEvent.click(screen.getByRole("button", { name: /get weather/i }));

    await waitFor(() => {
      expect(getCurrentWeather).toHaveBeenCalledWith("02108", expect.objectContaining({ defaultZipCode: "01830", unitSystem: "US" }), []);
    });

    expect(await screen.findByText(/washington, dc/i)).toBeInTheDocument();
    expect(screen.getByText(/best available walk/i)).toBeInTheDocument();
    expect(screen.getAllByText(/great · 86\/100/i).length).toBeGreaterThan(0);
    const breakdown = screen.getByLabelText(/overall wellness score breakdown/i);
    expect(within(breakdown).getByText("Weather")).toBeInTheDocument();
    expect(within(breakdown).getByText("Availability")).toBeInTheDocument();
    expect(within(breakdown).getByText("Preferences")).toBeInTheDocument();
    expect(screen.getByText(/free time/i)).toBeInTheDocument();
    expect(screen.getAllByText(/some chance of rain/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/30 min/i)).toBeInTheDocument();
    expect(screen.getByText(/fits your rain tolerance/i)).toBeInTheDocument();
    expect(screen.getAllByText(/high uv/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/score breakdown/i)).toBeInTheDocument();
    expect(screen.getByText(/temperature: 30\/30/i)).toBeInTheDocument();
    expect(screen.getByText(/rain: 16\/20/i)).toBeInTheDocument();
    expect(screen.getByText(/air quality: 10\/10/i)).toBeInTheDocument();
    expect(screen.getByText(/uv: 0\/10/i)).toBeInTheDocument();
    expect(screen.getByText(/environmental conditions/i)).toBeInTheDocument();
    expect(screen.getByText(/heat index/i)).toBeInTheDocument();
    expect(screen.getAllByText(/good/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/very high/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/4 hr 10 min/i)).toBeInTheDocument();
    expect(screen.getByText(/mostly sunny/i)).toBeInTheDocument();
    expect(screen.getByRole("img", { name: /mostly sunny weather icon/i })).toBeInTheDocument();
    expect(screen.getAllByRole("img", { name: /^sunny weather icon$/i }).length).toBeGreaterThan(0);
    expect(screen.getByRole("img", { name: /cloudy weather icon/i })).toBeInTheDocument();
    expect(screen.getByText(/^best$/i)).toBeInTheDocument();
    expect(screen.getAllByText(/not enough data/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/hourly forecast/i).length).toBeGreaterThan(1);
    expect(screen.getByText(/wind: 8 mph nw/i)).toBeInTheDocument();
    expect(screen.getByRole("list", { name: /seven-day walking outlook/i })).toBeInTheDocument();
    expect(screen.getByText(/monday/i)).toBeInTheDocument();
    expect(screen.getByText(/very hot/i)).toBeInTheDocument();
    expect(screen.getAllByText(/not enough hourly data/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/°f/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/unavailable/i).length).toBeGreaterThan(0);
    expect(screen.getByRole("list", { name: /hourly forecast/i })).toBeInTheDocument();
  });

  it("shows an accessible placeholder when a weather icon fails to load", async () => {
    vi.mocked(getCurrentWeather).mockResolvedValue({
      weather: {
        locationName: "Washington, DC",
        latitude: 38.9072,
        longitude: -77.0369,
        current: {
          temperature: 72.4,
          temperatureUnit: "°F",
          feelsLike: null,
          humidity: 50,
          windSpeed: 12,
          windDirection: "W",
          weatherCondition: "Mostly Sunny",
          iconUrl: "https://api.weather.gov/icons/land/day/few?size=medium",
          observationTime: "2024-01-01T12:00:00Z",
          dataType: "HOURLY_FORECAST",
        },
        environmentalConditions: null,
        bestWalkingWindow: null,
        hourlyForecast: [],
        weeklyOutlook: [],
      },
      error: null,
    });

    render(<WeatherSection />);

    fireEvent.change(screen.getByLabelText(/zip code/i), {
      target: { value: "02108" },
    });
    fireEvent.click(screen.getByRole("button", { name: /get weather/i }));

    await screen.findByRole("img", { name: /mostly sunny weather icon/i });
    const hiddenImage = document.querySelector("img[src='https://api.weather.gov/icons/land/day/few?size=medium']");
    expect(hiddenImage).not.toBeNull();
    fireEvent.error(hiddenImage as Element);

    expect(screen.getByRole("img", { name: /mostly sunny weather icon/i })).toHaveTextContent("NWS");
    expect(screen.getByText(/no walk recommendation available/i)).toBeInTheDocument();
  });

  it("rejects an invalid ZIP before calling the backend", async () => {
    render(<WeatherSection />);

    fireEvent.change(screen.getByLabelText(/zip code/i), {
      target: { value: "ABC" },
    });
    fireEvent.click(screen.getByRole("button", { name: /get weather/i }));

    expect(await screen.findByText(/valid 5-digit zip code/i)).toBeInTheDocument();
    expect(getCurrentWeather).not.toHaveBeenCalled();
  });

  it("shows a friendly backend unavailable message", async () => {
    vi.mocked(getCurrentWeather).mockResolvedValue({
      weather: null,
      error: "The ZIP lookup service is temporarily unavailable.",
    });

    render(<WeatherSection />);

    fireEvent.change(screen.getByLabelText(/zip code/i), {
      target: { value: "33139" },
    });
    fireEvent.click(screen.getByRole("button", { name: /get weather/i }));

    expect(await screen.findByText(/temporarily unavailable/i)).toBeInTheDocument();
  });

  it("preloads saved ZIP, sends saved preferences, converts metric display, and shows minimum score messaging", async () => {
    window.localStorage.setItem(
      settingsStorageKey,
      JSON.stringify({
        defaultZipCode: "90210",
        walkDurationMinutes: 45,
        preferredTimeOfDay: "EVENING",
        temperaturePreference: "WARMER",
        rainTolerance: "AVOID_RAIN",
        windTolerance: "LOW",
        minimumScore: 90,
        unitSystem: "METRIC",
      }),
    );

    vi.mocked(getCurrentWeather).mockResolvedValue({
      weather: {
        locationName: "Beverly Hills, CA",
        latitude: 34.0736,
        longitude: -118.4004,
        current: {
          temperature: 68,
          temperatureUnit: "°F",
          feelsLike: 68,
          humidity: 50,
          windSpeed: 10,
          windDirection: "W",
          weatherCondition: "Clear",
          iconUrl: null,
          observationTime: "2024-01-01T12:00:00Z",
          dataType: "HOURLY_FORECAST",
        },
        environmentalConditions: null,
        bestWalkingWindow: {
          startTime: "2024-01-01T18:00:00Z",
          endTime: "2024-01-01T18:45:00Z",
          score: 82,
          rating: "GREAT",
          ratingLabel: "Great",
          summary: "Great weather for a restorative walk.",
          positiveReasons: ["Comfortable feels-like temperature"],
          warnings: [],
          durationMinutes: 45,
          preferenceReasons: ["45-minute walk window", "Matches your preferred time of day"],
          minimumScore: 90,
          belowMinimumScore: true,
          minimumScoreMessage: "Best available window is below your minimum score of 90.",
          availability: "AVAILABLE",
          selectionReason: "Highest available weather score.",
          conflictingEvent: null,
          idealWeatherWindow: null,
          weatherScore: 82,
          availabilityScore: 100,
          preferenceScore: 75,
          overallScore: 85,
          calendarReasons: ["Selected window is fully available"],
          noAvailableReason: null,
        },
        hourlyForecast: [],
        weeklyOutlook: [
          {
            date: "2024-01-01",
            dayName: "Monday",
            iconUrl: null,
            shortForecast: "Clear",
            highTemperature: 68,
            lowTemperature: 50,
            temperatureUnit: "°F",
            precipitationProbability: 0,
            representativeScore: 82,
            rating: "GREAT",
            ratingLabel: "Great",
            bestAvailableTime: "2024-01-01T18:00:00Z",
            summary: "Great walking weather",
            environmentalWarnings: [],
          },
        ],
      },
      error: null,
    });

    render(<WeatherSection />);

    expect(await screen.findByDisplayValue("90210")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /get weather/i }));

    await waitFor(() => {
      expect(getCurrentWeather).toHaveBeenCalledWith("90210", expect.objectContaining({ unitSystem: "METRIC", walkDurationMinutes: 45 }), []);
    });

    expect(await screen.findByText(/beverly hills/i)).toBeInTheDocument();
    expect(screen.getAllByText(/20 °c/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/16 km\/h w/i)).toBeInTheDocument();
    expect(screen.getByText(/45 min/i)).toBeInTheDocument();
    expect(screen.getByText(/below your minimum score of 90/i)).toBeInTheDocument();
    expect(screen.getByText(/matches your preferred time of day/i)).toBeInTheDocument();
  });

  it("falls back to default settings when localStorage is malformed", async () => {
    window.localStorage.setItem(settingsStorageKey, "{not-json");
    render(<WeatherSection />);

    expect(await screen.findByDisplayValue("01830")).toBeInTheDocument();
  });
});
