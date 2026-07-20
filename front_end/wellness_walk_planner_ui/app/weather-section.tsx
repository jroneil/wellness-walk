"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { getCurrentWeather } from "./weather-actions";
import type {
  BestWalkingWindow,
  CalendarEvent,
  DailyOutlook,
  EnvironmentalConditions,
  HourlyForecastPeriod,
  UnitSystem,
  UserSettings,
  WalkingRecommendation,
  WeatherResponse,
} from "./types";
import { defaultUserSettings, loadSettings } from "./settings/settings";
import { loadCalendarEvents } from "./calendar/calendar-service";
import { processRecommendation } from "./notifications/notification-service";
import { OpportunityTimeline } from "./timeline/opportunity-timeline";
import { ActiveWalk } from "./walks/active-walk";

const initialZip = defaultUserSettings.defaultZipCode;

function toReadableLabel(value: string | null | undefined): string {
  if (!value) return "Unavailable";
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatTime(value: string | null | undefined): string {
  if (!value) return "Unavailable";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Unavailable";
  }
  return new Intl.DateTimeFormat("en", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return "Unavailable";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Unavailable";
  }
  return new Intl.DateTimeFormat("en", {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return "Unavailable";
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) {
    return "Unavailable";
  }
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
  }).format(date);
}

function displayTemperature(value: number | null | undefined, unitSystem: UnitSystem): { value: number | null; unit: string } {
  if (value == null) {
    return { value: null, unit: unitSystem === "METRIC" ? "°C" : "°F" };
  }
  return unitSystem === "METRIC"
    ? { value: (value - 32) * (5 / 9), unit: "°C" }
    : { value, unit: "°F" };
}

function formatValue(value: number | null | undefined): string {
  return value == null ? "Unavailable" : `${value}`;
}

function formatTemperature(value: number | null | undefined, unitSystem: UnitSystem): string {
  const temperature = displayTemperature(value, unitSystem);
  return temperature.value == null ? "Unavailable" : `${temperature.value.toFixed(0)} ${temperature.unit}`;
}

function formatWind(value: number | null | undefined, direction: string | null | undefined, unitSystem: UnitSystem): string {
  if (value == null) return "Unavailable";
  const converted = unitSystem === "METRIC" ? value * 1.60934 : value;
  const unit = unitSystem === "METRIC" ? "km/h" : "mph";
  return `${converted.toFixed(0)} ${unit}${direction ? ` ${direction}` : ""}`;
}

function formatDaylightMinutes(value: number | null | undefined): string {
  if (value == null) return "Unavailable";
  if (value <= 0) return "No daylight remaining";
  const hours = Math.floor(value / 60);
  const minutes = value % 60;
  if (hours === 0) return `${minutes} min`;
  if (minutes === 0) return `${hours} hr`;
  return `${hours} hr ${minutes} min`;
}

function formatScore(value: number | null | undefined): string {
  return value == null ? "Not enough data" : `${value}/100`;
}

function ratingTone(rating: string | null | undefined): string {
  if (rating === "EXCELLENT" || rating === "GREAT") {
    return "border-emerald-200 bg-emerald-50 text-emerald-800";
  }
  if (rating === "GOOD") {
    return "border-sky-200 bg-sky-50 text-sky-800";
  }
  if (rating === "FAIR") {
    return "border-amber-200 bg-amber-50 text-amber-800";
  }
  if (rating === "POOR" || rating === "NOT_RECOMMENDED") {
    return "border-rose-200 bg-rose-50 text-rose-800";
  }
  return "border-slate-200 bg-slate-100 text-slate-700";
}

function WeatherIcon({
  src,
  description,
  size = "md",
}: {
  src: string | null | undefined;
  description: string | null | undefined;
  size?: "sm" | "md";
}) {
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const timeoutRef = useRef<number | null>(null);
  const label = description ? `${description} weather icon` : "Weather icon unavailable";
  const sizeClass = size === "sm" ? "h-14 w-14" : "h-20 w-20";

  useEffect(() => {
    setFailed(false);
    setLoaded(false);
    if (timeoutRef.current != null) {
      window.clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    if (!src) return;
    timeoutRef.current = window.setTimeout(() => {
      setFailed(true);
    }, 8000);
    return () => {
      if (timeoutRef.current != null) {
        window.clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, [src]);

  function handleLoad() {
    if (timeoutRef.current != null) {
      window.clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    setLoaded(true);
  }

  if (!src || failed) {
    return (
      <div
        role="img"
        aria-label={label}
        className={`${sizeClass} flex shrink-0 items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-100 text-xs font-semibold text-slate-500`}
      >
        NWS
      </div>
    );
  }

  if (!loaded) {
    return (
      <div className={`${sizeClass} relative shrink-0`}>
        <div
          role="img"
          aria-label={label}
          className={`${sizeClass} flex items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-100 text-xs font-semibold text-slate-500`}
        >
          NWS
        </div>
        <img
          src={src}
          alt=""
          loading="lazy"
          decoding="async"
          onLoad={handleLoad}
          onError={() => setFailed(true)}
          className="absolute inset-0 h-px w-px opacity-0"
        />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={label}
      loading="lazy"
      decoding="async"
      onLoad={handleLoad}
      onError={() => setFailed(true)}
      className={`${sizeClass} shrink-0 object-contain`}
    />
  );
}

function RecommendationHero({ bestWindow }: { bestWindow: BestWalkingWindow | null }) {
  if (!bestWindow) {
    return (
      <div className="mt-8 rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-6">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-700">Today&apos;s Best Available Walk</p>
        <h3 className="mt-2 text-2xl font-semibold text-slate-900">No walk recommendation available</h3>
        <p className="mt-2 text-sm text-slate-600">The forecast does not include enough scorable upcoming weather data yet.</p>
      </div>
    );
  }

  if (bestWindow.noAvailableReason) {
    return (
      <div className="mt-8 rounded-3xl border border-amber-200 bg-amber-50 p-6">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-amber-800">Today&apos;s Best Available Walk</p>
        <h3 className="mt-2 text-2xl font-semibold text-slate-900">No available walking window</h3>
        <p className="mt-2 text-sm text-slate-700">{bestWindow.noAvailableReason}</p>
      </div>
    );
  }

  return (
    <div className="mt-6 rounded-[2rem] border border-emerald-300 bg-gradient-to-br from-emerald-50 to-white p-5 shadow-[0_20px_50px_-28px_rgba(5,150,105,0.65)] sm:p-6">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-800">Today&apos;s Best Available Walk</p>
          <h3 className="mt-2 text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
            {formatTime(bestWindow.startTime)}-{formatTime(bestWindow.endTime)}
          </h3>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <span className={`rounded-full border px-3 py-1 text-sm font-semibold ${ratingTone(bestWindow.rating)}`}>
              {bestWindow.ratingLabel} · {bestWindow.score}/100
            </span>
            <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-semibold text-slate-700">
              {bestWindow.durationMinutes} min
            </span>
            <span className="rounded-full border border-emerald-300 bg-white px-3 py-1 text-sm font-semibold text-emerald-800">Available</span>
            <span className="text-sm text-slate-700">{bestWindow.summary}</span>
          </div>
          {bestWindow.belowMinimumScore && bestWindow.minimumScoreMessage ? (
            <p className="mt-2 rounded-2xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
              {bestWindow.minimumScoreMessage}
            </p>
          ) : null}
          <p className="mt-3 text-base font-medium leading-relaxed text-slate-700">{bestWindow.selectionReason}</p>
          <ActiveWalk start={bestWindow.startTime} end={bestWindow.endTime} />
          <div className="mt-3 grid grid-cols-3 gap-2 text-sm" aria-label="Overall wellness score breakdown">
            <div className="rounded-xl bg-white px-3 py-2"><span className="block text-xs text-slate-500">Weather</span><strong>{bestWindow.weatherScore}%</strong></div>
            <div className="rounded-xl bg-white px-3 py-2"><span className="block text-xs text-slate-500">Availability</span><strong>{bestWindow.availabilityScore}%</strong></div>
            <div className="rounded-xl bg-white px-3 py-2"><span className="block text-xs text-slate-500">Preferences</span><strong>{bestWindow.preferenceScore}%</strong></div>
          </div>
          {bestWindow.idealWeatherWindow?.availability === "UNAVAILABLE" ? (
            <div className="mt-3 rounded-2xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              <p className="font-semibold">Ideal weather: {formatTime(bestWindow.idealWeatherWindow.startTime)} · Unavailable</p>
              <p className="mt-1">{bestWindow.idealWeatherWindow.conflictingEvent?.title ?? "Calendar conflict"} · {bestWindow.idealWeatherWindow.score}/100</p>
              {bestWindow.idealWeatherWindow.conflictingEvent ? <p className="mt-1 text-xs uppercase tracking-wide text-rose-700">Source: {bestWindow.idealWeatherWindow.conflictingEvent.source === "CALDAV" ? "CalDAV" : "Manual"}</p> : null}
              <p className="mt-1">Next available: {formatTime(bestWindow.startTime)} · Score {bestWindow.score}</p>
            </div>
          ) : null}
        </div>
        <div className="grid gap-3 rounded-2xl border border-emerald-100 bg-white/75 p-4 text-sm lg:w-[360px]">
          <div>
            <p className="font-semibold text-slate-900">Warnings</p>
            {bestWindow.warnings.length > 0 ? (
              <ul className="mt-2 space-y-1 text-slate-700">
                {bestWindow.warnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            ) : (
              <p className="mt-2 text-slate-700">No weather warnings for this hour.</p>
            )}
          </div>
          {bestWindow.preferenceReasons.length > 0 ? (
            <div>
              <p className="font-semibold text-slate-900">Preferences</p>
              <ul className="mt-2 flex flex-wrap gap-2 text-slate-700">
                {bestWindow.preferenceReasons.map((reason) => (
                  <li key={reason} className="rounded-full border border-emerald-200 bg-white px-2 py-1 text-xs font-semibold text-emerald-800">
                    {reason}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function ScoreBreakdown({ recommendation }: { recommendation: WalkingRecommendation | null | undefined }) {
  if (!recommendation || recommendation.score == null) return null;
  return (
    <details className="group mt-4 rounded-2xl border border-slate-200 bg-white text-sm text-slate-700">
      <summary className="cursor-pointer list-none rounded-2xl px-4 py-3 font-semibold text-slate-900 outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2">Score breakdown <span className="ml-2 text-xs font-normal text-slate-500 group-open:hidden">Show details</span><span className="ml-2 hidden text-xs font-normal text-slate-500 group-open:inline">Hide details</span></summary>
      <div className="border-t border-slate-100 px-4 pb-4">
      <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-7">
        <div>Temperature: {recommendation.temperatureScore}/30</div>
        <div>Rain: {recommendation.precipitationScore}/20</div>
        <div>Wind: {recommendation.windScore}/10</div>
        <div>Humidity: {recommendation.humidityScore}/10</div>
        <div>Daylight: {recommendation.daylightScore}/10</div>
        <div>Air Quality: {recommendation.airQualityScore}/10</div>
        <div>UV: {recommendation.uvScore}/10</div>
      </div>
      <ul className="mt-3 grid gap-1 text-xs text-slate-600 sm:grid-cols-2">
        {recommendation.reasons.map((reason) => (
          <li key={reason}>{reason}</li>
        ))}
      </ul>
      </div>
    </details>
  );
}

function RecommendationComparisonCard({weather}:{weather:WeatherResponse}){const best=weather.bestWalkingWindow;const now=weather.hourlyForecast[0]?.walkingRecommendation;if(!best)return null;const whyNot=[...(now?.warnings??[]),...(now?.reasons??[])].slice(0,3);return <section className="mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><h3 className="text-lg font-semibold">Recommendation Comparison</h3><div className="mt-4 grid gap-4 sm:grid-cols-2"><div className="rounded-2xl bg-emerald-50 p-4"><p className="font-semibold text-emerald-900">Why this recommendation?</p><ul className="mt-2 list-disc pl-5 text-sm text-emerald-900">{best.positiveReasons.slice(0,3).map(reason=><li key={reason}>{reason}</li>)}<li>Calendar available for the full walking window.</li></ul></div><div className="rounded-2xl bg-amber-50 p-4"><p className="font-semibold text-amber-900">Why not now?</p><ul className="mt-2 list-disc pl-5 text-sm text-amber-900">{whyNot.length?whyNot.map(reason=><li key={reason}>{reason}</li>):<li>The selected available window has the stronger overall recommendation.</li>}</ul></div></div></section>}

function EnvironmentalConditionsPanel({
  conditions,
}: {
  conditions: EnvironmentalConditions | null | undefined;
}) {
  if (!conditions) return null;
  const items = [
    {
      label: "AQI",
      value: formatValue(conditions.aqi),
      detail: conditions.aqiCategory ?? "Unavailable",
    },
    {
      label: "UV Index",
      value: formatValue(conditions.uvIndex),
      detail: conditions.uvCategory ?? "Unavailable",
    },
    {
      label: "Sunrise",
      value: formatTime(conditions.sunrise),
      detail: "Local forecast time",
    },
    {
      label: "Sunset",
      value: formatTime(conditions.sunset),
      detail: "Local forecast time",
    },
    {
      label: "Remaining Daylight",
      value: formatDaylightMinutes(conditions.remainingDaylightMinutes),
      detail: toReadableLabel(conditions.daylightStatus),
    },
  ];

  return (
    <details className="group mt-6 rounded-3xl border border-slate-200 bg-white shadow-sm">
      <summary className="cursor-pointer list-none rounded-3xl p-5 text-lg font-semibold text-slate-900 outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2">Environmental Conditions <span className="ml-2 text-sm font-normal text-slate-500 group-open:hidden">Show details</span><span className="ml-2 hidden text-sm font-normal text-slate-500 group-open:inline">Hide details</span></summary>
      <div className="border-t border-slate-100 px-5 pb-5">
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
        {items.map((item) => (
          <div key={item.label} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{item.label}</p>
            <p className="mt-2 text-base font-semibold text-slate-900">{item.value}</p>
            <p className="mt-1 text-xs text-slate-600">{item.detail}</p>
          </div>
        ))}
      </div>
      </div>
    </details>
  );
}

function HourlyCard({
  hour,
  selected,
  unitSystem,
}: {
  hour: HourlyForecastPeriod;
  selected: boolean;
  unitSystem: UnitSystem;
}) {
  const recommendation = hour.walkingRecommendation;
  return (
    <li
      className={`min-w-[150px] flex-1 rounded-2xl border p-3 shadow-sm ${
        selected ? "border-emerald-300 bg-emerald-50 ring-2 ring-emerald-200" : "border-slate-200 bg-slate-50"
      }`}
      aria-label={`${formatTime(hour.startTime)} forecast, ${recommendation.ratingLabel}, ${formatScore(recommendation.score)}`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="text-sm font-semibold text-slate-900">{formatTime(hour.startTime)}</div>
        {selected ? <span className="rounded-full bg-emerald-700 px-2 py-0.5 text-xs font-semibold text-white">Best</span> : null}
      </div>
      <div className="mt-3">
        <WeatherIcon src={hour.iconUrl} description={hour.shortForecast} size="sm" />
      </div>
      <div className="mt-3 text-2xl font-semibold text-slate-900">
        {formatTemperature(hour.temperature, unitSystem)}
      </div>
      <div className="mt-2 text-sm text-slate-600">{hour.shortForecast || "Unavailable"}</div>
      <div className={`mt-3 rounded-full border px-2 py-1 text-xs font-semibold ${ratingTone(recommendation.rating)}`}>
        {recommendation.ratingLabel} · {formatScore(recommendation.score)}
      </div>
      <div className="mt-3 space-y-1 text-sm text-slate-600">
        <div>Precip: {hour.precipitationProbability != null ? `${hour.precipitationProbability}%` : "Unavailable"}</div>
        <div>Humidity: {hour.humidity != null ? `${hour.humidity}%` : "Unavailable"}</div>
        <div>Wind: {formatWind(hour.windSpeed, hour.windDirection, unitSystem)}</div>
      </div>
      {recommendation.warnings.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-1">
          {recommendation.warnings.slice(0, 3).map((warning) => (
            <span key={warning} className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-800">
              {warning}
            </span>
          ))}
        </div>
      ) : null}
    </li>
  );
}

function TodaySchedule({ events, bestWindow }: { events: CalendarEvent[]; bestWindow: BestWalkingWindow | null }) {
  const today = new Date().toDateString();
  const todayEvents = events.filter((event) => new Date(event.startTime).toDateString() === today);
  return (
    <section className="mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <h3 className="text-lg font-semibold text-slate-900">Today&apos;s Schedule</h3>
      <p className="mt-1 text-sm text-slate-600">Busy blocks and the selected walking window.</p>
      <ol aria-label="Today's schedule timeline" className="mt-5 space-y-2">
        {todayEvents.map((event) => (
          <li key={event.id} className={`rounded-2xl border-l-4 p-3 ${event.busy ? "border-rose-500 bg-rose-50" : "border-sky-500 bg-sky-50"}`}>
            <span className="font-semibold">{formatTime(event.startTime)}–{formatTime(event.endTime)}</span> · {event.title} · {event.busy ? "Busy" : "Free"}
          </li>
        ))}
        {!todayEvents.some((event) => event.busy) ? (
          <li className="rounded-2xl border-l-4 border-sky-500 bg-sky-50 p-3"><span className="font-semibold">Free time</span> · No busy events on today&apos;s calendar</li>
        ) : null}
        {bestWindow?.idealWeatherWindow?.availability === "UNAVAILABLE" ? (
          <li className="rounded-2xl border-l-4 border-amber-500 bg-amber-50 p-3">
            <span className="font-semibold">{formatTime(bestWindow.idealWeatherWindow.startTime)}–{formatTime(bestWindow.idealWeatherWindow.endTime)}</span> · Ideal weather, unavailable
          </li>
        ) : null}
        {bestWindow ? (
          <li className="rounded-2xl border-l-4 border-emerald-600 bg-emerald-50 p-3 ring-2 ring-emerald-200">
            <span className="font-semibold">{formatTime(bestWindow.startTime)}–{formatTime(bestWindow.endTime)}</span> · Recommended walk
          </li>
        ) : null}
      </ol>
      {!todayEvents.length && !bestWindow ? <p className="mt-5 text-sm text-slate-600">No events or recommendation to display.</p> : null}
    </section>
  );
}

function WeeklyOutlook({ days, unitSystem }: { days: DailyOutlook[]; unitSystem: UnitSystem }) {
  if (days.length === 0) return null;
  return (
    <div className="mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div>
        <h3 className="text-lg font-semibold text-slate-900">Seven-Day Walking Outlook</h3>
        <p className="mt-1 text-sm text-slate-600">Daily weather with a walking score when hourly data supports one.</p>
      </div>
      <ol aria-label="Seven-day walking outlook" className="mt-5 flex snap-x gap-3 overflow-x-auto pb-2 xl:grid xl:grid-cols-7 xl:overflow-visible">
        {days.map((day) => (
          <li key={day.date} className="min-w-[220px] snap-start rounded-2xl border border-slate-200 bg-slate-50 p-4 shadow-sm xl:min-w-0">
            <div className="flex items-start justify-between gap-2">
              <div><p className="font-semibold text-slate-900">{day.dayName}</p><p className="text-xs text-slate-500">{formatDate(day.date)}</p></div>
              <div className={`rounded-xl border px-2 py-1 text-right ${ratingTone(day.rating)}`}>
                <strong className="block text-lg leading-none">{day.representativeScore != null ? day.representativeScore : "—"}</strong>
                <span className="text-[11px] font-semibold">Score · {day.ratingLabel}</span>
              </div>
            </div>
            <div className="mt-4 flex items-center justify-between gap-3">
              <WeatherIcon src={day.iconUrl} description={day.shortForecast} size="sm" />
              <p className="text-right text-sm"><span className="font-semibold text-slate-900">H {formatTemperature(day.highTemperature, unitSystem)}</span><br/><span className="text-slate-600">L {formatTemperature(day.lowTemperature, unitSystem)}</span></p>
            </div>
            <div className="mt-4 rounded-xl bg-white px-3 py-2">
              <p className="text-xs font-medium text-slate-500">Best walking time</p>
              <p className="mt-0.5 font-semibold text-slate-900">{day.bestAvailableTime ? formatTime(day.bestAvailableTime) : "Not enough hourly data"}</p>
            </div>
            <details className="group mt-3 border-t border-slate-200 pt-2 text-sm">
              <summary className="cursor-pointer list-none font-medium text-slate-600 outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2">More conditions <span className="group-open:hidden">+</span><span className="hidden group-open:inline">−</span></summary>
              <div className="mt-2 space-y-2 text-slate-600">
                <p>{day.shortForecast || "Unavailable"}</p>
                <p>Precipitation: {day.precipitationProbability != null ? `${day.precipitationProbability}%` : "Unavailable"}</p>
                {day.environmentalWarnings.length > 0 ? <div className="flex flex-wrap gap-1">{day.environmentalWarnings.map(warning => <span key={warning} className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-800">{warning}</span>)}</div> : null}
              </div>
            </details>
          </li>
        ))}
      </ol>
    </div>
  );
}

export default function WeatherSection() {
  const [zipCode, setZipCode] = useState(initialZip);
  const [settings, setSettings] = useState<UserSettings>(defaultUserSettings);
  const [calendarEvents, setCalendarEvents] = useState<CalendarEvent[]>([]);
  const [weather, setWeather] = useState<WeatherResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const current = weather?.current;
  const hourlyForecast = useMemo(() => weather?.hourlyForecast ?? [], [weather?.hourlyForecast]);
  const selectedStartTime = weather?.bestWalkingWindow?.startTime;
  const selectedRecommendation = hourlyForecast.find((hour) => {
    if (!selectedStartTime) return false;
    const selected = new Date(selectedStartTime).getTime();
    const start = new Date(hour.startTime).getTime();
    return selected >= start && selected < start + 60 * 60 * 1000;
  })?.walkingRecommendation;

  useEffect(() => {
    const savedSettings = loadSettings();
    setSettings(savedSettings);
    setZipCode(savedSettings.defaultZipCode);
    setCalendarEvents(loadCalendarEvents());
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedZip = zipCode.trim();
    if (!/^\d{5}$/.test(trimmedZip)) {
      setError("Enter a valid 5-digit ZIP code.");
      setWeather(null);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const events = loadCalendarEvents();
      setCalendarEvents(events);
      const response = await getCurrentWeather(trimmedZip, settings, events);
      setWeather(response.weather);
      setError(response.error);
      if(response.weather){sessionStorage.setItem("wellness-window-last-weather:v1",JSON.stringify(response.weather));void processRecommendation(response.weather,events,settings);}
    } catch {
      setError("Weather request failed. Please try again.");
      setWeather(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-[0_16px_45px_-24px_rgba(15,23,42,0.35)] sm:p-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">Weather</p>
          <h2 className="mt-1 text-2xl font-semibold text-slate-900">Current conditions</h2>
          <p className="mt-2 max-w-2xl text-sm text-slate-600">
            Enter a ZIP code to see the current weather and the next several hours in a calmer, more readable layout.
          </p>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
            ZIP Code
            <input
              aria-label="ZIP Code"
              value={zipCode}
              onChange={(event) => setZipCode(event.target.value)}
              className="w-full rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 shadow-sm outline-none transition focus:border-emerald-500 focus:bg-white sm:w-36"
              inputMode="numeric"
              maxLength={5}
              placeholder="01830"
            />
          </label>
          <button
            type="submit"
            className="rounded-2xl bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-800"
          >
            {loading ? "Loading..." : "Get weather"}
          </button>
        </form>
      </div>

      {error ? (
        <p className="mt-6 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>
      ) : null}

      {!weather && !loading && !error ? (
        <div className="mt-8 rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-600">
          Search by ZIP code to view the current weather and the next hours of the day.
        </div>
      ) : null}

      {loading ? (
        <div className="mt-8 rounded-3xl border border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-600">
          Loading weather for {zipCode}...
        </div>
      ) : null}

      {weather && current ? (
        <>
          <RecommendationHero bestWindow={weather.bestWalkingWindow} />
          <ScoreBreakdown recommendation={selectedRecommendation} />
          <div className="grid gap-6 lg:grid-cols-2">
            <RecommendationComparisonCard weather={weather}/>
            <TodaySchedule events={calendarEvents} bestWindow={weather.bestWalkingWindow} />
          </div>
          <OpportunityTimeline weather={weather} events={calendarEvents}/>
          <EnvironmentalConditionsPanel conditions={weather.environmentalConditions} />

          <div className="mt-8 grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
            <section className="min-w-0 rounded-3xl border border-slate-200 bg-slate-50 p-5" aria-labelledby="current-weather-title">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <WeatherIcon src={current.iconUrl} description={current.weatherCondition} />
                  <div>
                    <p id="current-weather-title" className="text-sm font-semibold text-slate-600">Current weather · {weather.locationName}</p>
                    <div className="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
                      <p className="text-4xl font-semibold text-slate-900">{formatTemperature(current.temperature, settings.unitSystem)}</p>
                      <p className="text-base font-semibold text-slate-800">{current.weatherCondition || "Unavailable"}</p>
                    </div>
                    {current.feelsLike != null ? <p className="mt-1 text-sm text-slate-600">Feels like {formatTemperature(current.feelsLike, settings.unitSystem)}{weather.environmentalConditions?.feelsLikeMethod ? ` · ${toReadableLabel(weather.environmentalConditions.feelsLikeMethod)}` : ""}</p> : null}
                  </div>
                </div>
                <span className="rounded-full border border-emerald-200 bg-white px-3 py-1 text-xs font-medium text-emerald-700">{toReadableLabel(current.dataType)}</span>
              </div>
              <details className="group mt-4 border-t border-slate-200 pt-3">
                <summary className="cursor-pointer list-none text-sm font-semibold text-slate-700 outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2">Weather details <span className="font-normal text-slate-500 group-open:hidden">· Humidity, wind, and observation time</span><span className="hidden font-normal text-slate-500 group-open:inline">· Hide details</span></summary>
                <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
                  <div><dt className="text-slate-500">Humidity</dt><dd className="mt-0.5 font-semibold text-slate-900">{formatValue(current.humidity)}</dd></div>
                  <div><dt className="text-slate-500">Wind</dt><dd className="mt-0.5 font-semibold text-slate-900">{formatWind(current.windSpeed, current.windDirection, settings.unitSystem)}</dd></div>
                  <div><dt className="text-slate-500">Observed</dt><dd className="mt-0.5 font-semibold text-slate-900">{formatDateTime(current.observationTime)}</dd></div>
                </dl>
              </details>
            </section>

            <details className="group min-w-0 rounded-3xl border border-slate-200 bg-white shadow-sm">
              <summary className="cursor-pointer list-none rounded-3xl p-6 outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">Hourly Forecast</h3>
                  <p className="mt-1 text-sm text-slate-600"><span className="group-open:hidden">Show the detailed hourly forecast.</span><span className="hidden group-open:inline">Hide the detailed hourly forecast.</span></p>
                </div>
              </div>
              </summary>
              <div className="border-t border-slate-100 px-6 pb-6">
              <ol aria-label="Hourly forecast" className="mt-6 flex w-full max-w-full gap-3 overflow-x-auto pb-2">
                {hourlyForecast.map((hour, index) => (
                  <HourlyCard key={`${hour.startTime}-${index}`} hour={hour} selected={hour.startTime === selectedStartTime} unitSystem={settings.unitSystem} />
                ))}
              </ol>
              </div>
            </details>
          </div>
          <WeeklyOutlook days={weather.weeklyOutlook} unitSystem={settings.unitSystem} />
        </>
      ) : null}
    </section>
  );
}
