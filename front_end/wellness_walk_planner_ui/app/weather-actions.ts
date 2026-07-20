"use server";

import { fetchWeatherCurrent } from "./lib/backend";
import type { CalendarEvent, UserSettings, WeatherResponse } from "./types";

export interface WeatherActionState {
  weather: WeatherResponse | null;
  error: string | null;
}

export async function getCurrentWeather(zipCode: string, settings?: UserSettings, calendarEvents: CalendarEvent[] = []): Promise<WeatherActionState> {
  const trimmedZip = zipCode.trim();
  if (!/^\d{5}$/.test(trimmedZip)) {
    return {
      weather: null,
      error: "Enter a valid 5-digit ZIP code.",
    };
  }

  try {
    return {
      weather: await fetchWeatherCurrent(trimmedZip, settings, calendarEvents),
      error: null,
    };
  } catch (err) {
    return {
      weather: null,
      error: err instanceof Error ? err.message : "Weather request failed. Please try again.",
    };
  }
}
