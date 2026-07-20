export interface BackendStatusResponse {
  applicationName: string;
  status: string;
  backendTimestamp: string;
  developmentStage: string;
}

export interface CurrentWeatherSummary {
  temperature: number;
  temperatureUnit: string;
  feelsLike: number | null;
  humidity: number | null;
  windSpeed: number | null;
  windDirection: string | null;
  weatherCondition: string;
  iconUrl: string | null;
  observationTime: string;
  dataType: string;
}

export interface WalkingRecommendation {
  startTime: string;
  score: number | null;
  rating: string | null;
  ratingLabel: string;
  recommended: boolean;
  temperatureScore: number | null;
  precipitationScore: number | null;
  windScore: number | null;
  humidityScore: number | null;
  daylightScore: number | null;
  airQualityScore: number | null;
  uvScore: number | null;
  feelsLikeTemperature: number | null;
  feelsLikeMethod: string | null;
  reasons: string[];
  warnings: string[];
}

export interface BestWalkingWindow {
  startTime: string;
  endTime: string;
  score: number;
  rating: string;
  ratingLabel: string;
  summary: string;
  positiveReasons: string[];
  warnings: string[];
  durationMinutes: number;
  preferenceReasons: string[];
  minimumScore: number;
  belowMinimumScore: boolean;
  minimumScoreMessage: string | null;
  availability: "AVAILABLE" | "UNAVAILABLE";
  selectionReason: string;
  conflictingEvent: CalendarConflict | null;
  idealWeatherWindow: IdealWeatherWindow | null;
  weatherScore: number;
  availabilityScore: number;
  preferenceScore: number;
  overallScore: number;
  calendarReasons: string[];
  noAvailableReason: string | null;
}

export type CalendarSource = "MANUAL" | "CALDAV" | "GOOGLE" | "MICROSOFT";
export type CalendarProviderType = "MANUAL" | "CALDAV" | "GOOGLE" | "MICROSOFT";

export interface CalendarEvent {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
  busy: boolean;
  source: CalendarSource;
  providerType?: CalendarProviderType;
  providerEventId?: string;
  calendarId?: string;
  occurrenceId?: string;
  timezone?: string;
  allDay?: boolean;
}

export interface CalendarProvider {
  type: CalendarProviderType;
  displayName: string;
  installationConfigured: boolean;
  enabled: boolean;
  connected: boolean;
  authorizationRequired: boolean;
  selectedCalendarCount: number;
  discoveredCalendarCount: number;
  lastSuccessfulSyncAt: string | null;
  lastAttemptedSyncAt: string | null;
  providerStatus: "DISABLED" | "DISCONNECTED" | "CONFIGURATION_REQUIRED" | "AUTHORIZATION_PENDING" | "AUTHORIZATION_REQUIRED" | "CONNECTED" | "ERROR" | "UNAVAILABLE" | "COMING_LATER";
  safeMessage: string;
  status: "DISABLED" | "DISCONNECTED" | "CONFIGURATION_REQUIRED" | "AUTHORIZATION_PENDING" | "AUTHORIZATION_REQUIRED" | "CONNECTED" | "ERROR" | "UNAVAILABLE" | "COMING_LATER";
  capabilities: { readEvents: boolean; writeEvents: boolean; discovery: boolean; recurrence: boolean };
}

export interface CalendarProviderSnapshot {
  providers: CalendarProvider[];
  available: boolean;
  message: string | null;
}

export interface CalendarConnectionResult { status: CalendarProvider["status"]; message: string }
export interface CalendarDescriptor { providerType: CalendarProviderType; calendarId: string; displayName: string; calendarUrl: string; enabled: boolean; readOnly: boolean; supportsEvents: boolean; selected: boolean; color: string | null; description: string | null }
export interface CalendarDiscoveryResult { calendars: CalendarDescriptor[]; status: "DISCOVERED" | "SELECTION_REQUIRED" | "NO_CALENDARS" | "EXPLICIT_PATH" | "DISABLED"; message: string; discoveredAt: string }
export interface CalendarSyncDetail { providerType: CalendarProviderType; calendarId: string | null; status: string; eventCount: number; errorCode: string | null; message: string }
export interface CalendarProviderSyncStatus { lastAttempt: string | null; lastSuccess: string | null; status: string; message: string; calendars: CalendarSyncDetail[] }
export interface CalendarSyncResult { events: CalendarEvent[]; errors: { providerType: CalendarProviderType; code: string; message: string }[]; calendars: CalendarSyncDetail[]; synchronizedAt: string; partial: boolean }
export interface OAuthAuthorizationStart { authorizationUrl: string; expiresAt: string }

export interface CalendarConflict {
  eventId: string;
  title: string;
  startTime: string;
  endTime: string;
  source: CalendarSource;
}

export interface IdealWeatherWindow {
  startTime: string;
  endTime: string;
  score: number;
  availability: "AVAILABLE" | "UNAVAILABLE";
  conflictingEvent: CalendarConflict | null;
}

export interface HourlyForecastPeriod {
  startTime: string;
  temperature: number;
  actualTemperature: number | null;
  temperatureUnit: string;
  shortForecast: string;
  iconUrl: string | null;
  precipitationProbability: number | null;
  humidity: number | null;
  windSpeed: number | null;
  windDirection: string | null;
  isDaytime: boolean | null;
  feelsLikeTemperature: number | null;
  feelsLikeMethod: string | null;
  uvIndex: number | null;
  uvCategory: string | null;
  uvObservationOrForecastTime: string | null;
  uvSource: string | null;
  aqi: number | null;
  aqiCategory: string | null;
  aqiObservationTime: string | null;
  aqiSource: string | null;
  sunrise: string | null;
  sunset: string | null;
  daylightStatus: string | null;
  remainingDaylightMinutes: number | null;
  walkingRecommendation: WalkingRecommendation;
}

export interface DailyOutlook {
  date: string;
  dayName: string;
  iconUrl: string | null;
  shortForecast: string | null;
  highTemperature: number | null;
  lowTemperature: number | null;
  temperatureUnit: string;
  precipitationProbability: number | null;
  representativeScore: number | null;
  rating: string | null;
  ratingLabel: string;
  bestAvailableTime: string | null;
  summary: string;
  environmentalWarnings: string[];
}

export interface EnvironmentalConditions {
  actualTemperature: number | null;
  feelsLikeTemperature: number | null;
  temperatureUnit: string | null;
  feelsLikeMethod: string | null;
  aqi: number | null;
  aqiCategory: string | null;
  aqiObservationTime: string | null;
  aqiSource: string | null;
  uvIndex: number | null;
  uvCategory: string | null;
  uvObservationOrForecastTime: string | null;
  uvSource: string | null;
  sunrise: string | null;
  sunset: string | null;
  daylightStatus: string | null;
  remainingDaylightMinutes: number | null;
}

export interface WeatherResponse {
  locationName: string;
  latitude: number;
  longitude: number;
  current: CurrentWeatherSummary;
  environmentalConditions: EnvironmentalConditions | null;
  bestWalkingWindow: BestWalkingWindow | null;
  hourlyForecast: HourlyForecastPeriod[];
  weeklyOutlook: DailyOutlook[];
}

export type PreferredTimeOfDay = "ANY" | "MORNING" | "LUNCH" | "AFTERNOON" | "EVENING";
export type TemperaturePreference = "COOLER" | "BALANCED" | "WARMER";
export type RainTolerance = "AVOID_RAIN" | "LIGHT_RAIN_OK" | "RAIN_OK";
export type WindTolerance = "LOW" | "MODERATE" | "HIGH";
export type UnitSystem = "US" | "METRIC";

export interface UserSettings {
  defaultZipCode: string;
  walkDurationMinutes: 10 | 15 | 20 | 30 | 45 | 60;
  preferredTimeOfDay: PreferredTimeOfDay;
  temperaturePreference: TemperaturePreference;
  rainTolerance: RainTolerance;
  windTolerance: WindTolerance;
  minimumScore: number;
  unitSystem: UnitSystem;
  notificationsEnabled: boolean;
  notificationLeadTimeMinutes: 5 | 10 | 15 | 30;
  minimumNotificationScore: number;
  quietHoursEnabled: boolean;
  quietHoursStart: string;
  quietHoursEnd: string;
  notifyOnWeekends: boolean;
  workingHoursOnly: boolean;
  maximumNotificationsPerDay: number;
  notificationCooldownMinutes: number;
}

export interface RecommendationSnapshot { timestamp:string;score:number;recommendedStart:string;recommendedEnd:string;temperature:number|null;wind:number|null;humidity:number|null;aqi:number|null;uv:number|null;calendarAvailable:boolean;providerSources:string[];reasonSummary:string }
export interface HistorySummary { averageRecommendation:number;highestScore:number|null;bestTime:string|null;opportunities:number;calendarConflicts:number;weatherConflicts:number;missedOpportunities:number;bestDay:string|null;mostBlockedDay:string|null;averageTemperature:number|null;averageAqi:number|null;averageUv:number|null;providerSources:string[] }
export interface RecommendationComparison { previous:RecommendationSnapshot;current:RecommendationSnapshot;scoreDifference:number;significantReasons:string[] }
export interface HistoryResponse { snapshots:RecommendationSnapshot[];today:HistorySummary;week:HistorySummary;comparison:RecommendationComparison|null }
export interface NotificationDecision { eligible:boolean;scheduleAt:string|null;reason:string;title:string|null;body:string|null }
export type ActivityStatus="ACTIVE"|"COMPLETED"|"PARTIALLY_COMPLETED"|"SKIPPED"|"DISMISSED"|"EXPIRED"|"UNKNOWN";
export type ActivitySource="DASHBOARD"|"NOTIFICATION"|"HISTORY"|"TIMELINE"|"MANUAL_ENTRY";
export type PerceivedQuality="RESTORATIVE"|"GOOD"|"NEUTRAL"|"UNCOMFORTABLE";
export interface WalkActivity{id:string;recommendationHistoryId:string|null;opportunityStart:string;opportunityEnd:string;activityStatus:ActivityStatus;actualStart:string|null;actualEnd:string|null;durationMinutes:number|null;perceivedQuality:PerceivedQuality|null;notes:string|null;source:ActivitySource;createdAt:string;updatedAt:string}
export interface WellnessGoal{enabled:boolean;weeklyMinutesTarget:number|null;weeklyWalkCountTarget:number|null;minimumQualifyingMinutes:number|null;weekStartsOn:string;timezone:string}
export interface GoalProgress{enabled:boolean;completedWalks:number;walkTarget:number|null;walkingMinutes:number;minutesTarget:number|null;remainingMinutes:number;activeDays:number;averageDuration:number|null;mostCommonPeriod:string|null}
export interface HistoryRetention{recommendationDays:number|null;notificationDays:number|null;expiredOutcomeDays:number|null;activityDays:number|null}
