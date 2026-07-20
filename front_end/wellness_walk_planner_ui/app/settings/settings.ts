import type { UserSettings } from "../types";

export const settingsStorageKey = "wellness-window-settings:v1";

export const defaultUserSettings: UserSettings = {
  defaultZipCode: "01830",
  walkDurationMinutes: 30,
  preferredTimeOfDay: "ANY",
  temperaturePreference: "BALANCED",
  rainTolerance: "LIGHT_RAIN_OK",
  windTolerance: "MODERATE",
  minimumScore: 60,
  unitSystem: "US",
  notificationsEnabled: false,
  notificationLeadTimeMinutes: 15,
  minimumNotificationScore: 80,
  quietHoursEnabled: false,
  quietHoursStart: "22:00",
  quietHoursEnd: "07:00",
  notifyOnWeekends: true,
  workingHoursOnly: false,
  maximumNotificationsPerDay: 3,
  notificationCooldownMinutes: 60,
};

const allowedDurations = [10, 15, 20, 30, 45, 60] as const;
const preferredTimes = ["ANY", "MORNING", "LUNCH", "AFTERNOON", "EVENING"] as const;
const temperaturePreferences = ["COOLER", "BALANCED", "WARMER"] as const;
const rainTolerances = ["AVOID_RAIN", "LIGHT_RAIN_OK", "RAIN_OK"] as const;
const windTolerances = ["LOW", "MODERATE", "HIGH"] as const;
const unitSystems = ["US", "METRIC"] as const;
const leadTimes = [5,10,15,30] as const;

export function sanitizeSettings(value: unknown): UserSettings {
  if (!value || typeof value !== "object") {
    return defaultUserSettings;
  }
  const candidate = value as Partial<UserSettings>;
  return {
    defaultZipCode: validZip(candidate.defaultZipCode) ? candidate.defaultZipCode : defaultUserSettings.defaultZipCode,
    walkDurationMinutes: includesNumber(allowedDurations, candidate.walkDurationMinutes)
      ? candidate.walkDurationMinutes
      : defaultUserSettings.walkDurationMinutes,
    preferredTimeOfDay: includesString(preferredTimes, candidate.preferredTimeOfDay)
      ? candidate.preferredTimeOfDay
      : defaultUserSettings.preferredTimeOfDay,
    temperaturePreference: includesString(temperaturePreferences, candidate.temperaturePreference)
      ? candidate.temperaturePreference
      : defaultUserSettings.temperaturePreference,
    rainTolerance: includesString(rainTolerances, candidate.rainTolerance)
      ? candidate.rainTolerance
      : defaultUserSettings.rainTolerance,
    windTolerance: includesString(windTolerances, candidate.windTolerance)
      ? candidate.windTolerance
      : defaultUserSettings.windTolerance,
    minimumScore: validMinimumScore(candidate.minimumScore) ? candidate.minimumScore : defaultUserSettings.minimumScore,
    unitSystem: includesString(unitSystems, candidate.unitSystem) ? candidate.unitSystem : defaultUserSettings.unitSystem,
    notificationsEnabled: typeof candidate.notificationsEnabled === "boolean" ? candidate.notificationsEnabled : defaultUserSettings.notificationsEnabled,
    notificationLeadTimeMinutes: includesNumber(leadTimes,candidate.notificationLeadTimeMinutes)?candidate.notificationLeadTimeMinutes:defaultUserSettings.notificationLeadTimeMinutes,
    minimumNotificationScore: validMinimumScore(candidate.minimumNotificationScore)?candidate.minimumNotificationScore:defaultUserSettings.minimumNotificationScore,
    quietHoursEnabled: typeof candidate.quietHoursEnabled === "boolean"?candidate.quietHoursEnabled:defaultUserSettings.quietHoursEnabled,
    quietHoursStart: validTime(candidate.quietHoursStart)?candidate.quietHoursStart:defaultUserSettings.quietHoursStart,
    quietHoursEnd: validTime(candidate.quietHoursEnd)?candidate.quietHoursEnd:defaultUserSettings.quietHoursEnd,
    notifyOnWeekends: typeof candidate.notifyOnWeekends === "boolean"?candidate.notifyOnWeekends:defaultUserSettings.notifyOnWeekends,
    workingHoursOnly: typeof candidate.workingHoursOnly === "boolean"?candidate.workingHoursOnly:defaultUserSettings.workingHoursOnly,
    maximumNotificationsPerDay: validRange(candidate.maximumNotificationsPerDay,1,20)?candidate.maximumNotificationsPerDay:defaultUserSettings.maximumNotificationsPerDay,
    notificationCooldownMinutes: validRange(candidate.notificationCooldownMinutes,5,1440)?candidate.notificationCooldownMinutes:defaultUserSettings.notificationCooldownMinutes,
  };
}

export function loadSettings(): UserSettings {
  if (typeof window === "undefined") {
    return defaultUserSettings;
  }
  try {
    const raw = window.localStorage.getItem(settingsStorageKey);
    return raw ? sanitizeSettings(JSON.parse(raw)) : defaultUserSettings;
  } catch {
    return defaultUserSettings;
  }
}

export function saveSettings(settings: UserSettings) {
  window.localStorage.setItem(settingsStorageKey, JSON.stringify(sanitizeSettings(settings)));
}

export function resetSettings() {
  window.localStorage.removeItem(settingsStorageKey);
}

export function validateSettings(settings: UserSettings): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!validZip(settings.defaultZipCode)) {
    errors.defaultZipCode = "Enter a valid 5-digit ZIP code.";
  }
  if (!validMinimumScore(settings.minimumScore)) {
    errors.minimumScore = "Minimum score must be between 0 and 100.";
  }
  if(!validMinimumScore(settings.minimumNotificationScore))errors.minimumNotificationScore="Notification score must be between 0 and 100.";
  if(!validRange(settings.maximumNotificationsPerDay,1,20))errors.maximumNotificationsPerDay="Choose between 1 and 20 notifications.";
  return errors;
}
function validTime(value:unknown):value is string{return typeof value==="string"&&/^([01]\d|2[0-3]):[0-5]\d$/.test(value);}
function validRange(value:unknown,min:number,max:number):value is number{return typeof value==="number"&&Number.isInteger(value)&&value>=min&&value<=max;}

function validZip(value: unknown): value is string {
  return typeof value === "string" && /^\d{5}$/.test(value);
}

function validMinimumScore(value: unknown): value is number {
  return typeof value === "number" && Number.isInteger(value) && value >= 0 && value <= 100;
}

function includesString<T extends readonly string[]>(allowed: T, value: unknown): value is T[number] {
  return typeof value === "string" && allowed.includes(value);
}

function includesNumber<T extends readonly number[]>(allowed: T, value: unknown): value is T[number] {
  return typeof value === "number" && allowed.includes(value);
}
