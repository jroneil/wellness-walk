import type { CalendarEvent } from "../types";

export interface CalendarService {
  list(): CalendarEvent[];
  save(event: CalendarEvent): CalendarEvent[];
  delete(id: string): CalendarEvent[];
}

export const calendarStorageKey = "wellness-window-calendar:v2";
export const externalCalendarSessionKey = "wellness-window-external-calendar:v1";

export function sanitizeCalendarEvents(value: unknown): CalendarEvent[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter(isCalendarEvent)
    .map((event) => ({ ...event, source: event.source ?? "MANUAL" }))
    .sort((left, right) => left.startTime.localeCompare(right.startTime));
}

export function loadCalendarEvents(): CalendarEvent[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(calendarStorageKey);
    const manual = raw ? sanitizeCalendarEvents(JSON.parse(raw)) : [];
    const externalRaw = window.sessionStorage.getItem(externalCalendarSessionKey);
    return sanitizeCalendarEvents([...manual, ...(externalRaw ? JSON.parse(externalRaw) : [])]);
  } catch {
    return [];
  }
}

export function saveCalendarEvent(event: CalendarEvent): CalendarEvent[] {
  const events = loadManualEvents().filter((candidate) => candidate.id !== event.id);
  const next = sanitizeCalendarEvents([...events, event]);
  window.localStorage.setItem(calendarStorageKey, JSON.stringify(next));
  return loadCalendarEvents();
}

export function deleteCalendarEvent(id: string): CalendarEvent[] {
  const next = loadManualEvents().filter((event) => event.id !== id);
  window.localStorage.setItem(calendarStorageKey, JSON.stringify(next));
  return loadCalendarEvents();
}

export function saveExternalCalendarEvents(events: CalendarEvent[]): CalendarEvent[] {
  window.sessionStorage.setItem(externalCalendarSessionKey, JSON.stringify(sanitizeCalendarEvents(events.filter((event) => event.source !== "MANUAL"))));
  return loadCalendarEvents();
}

function loadManualEvents(): CalendarEvent[] {
  try { const raw = window.localStorage.getItem(calendarStorageKey); return raw ? sanitizeCalendarEvents(JSON.parse(raw)) : []; }
  catch { return []; }
}

export function validateCalendarEvent(event: CalendarEvent): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!event.title.trim()) errors.title = "Enter an event title.";
  const start = new Date(event.startTime);
  const end = new Date(event.endTime);
  if (Number.isNaN(start.getTime())) errors.startTime = "Choose a valid start time.";
  if (Number.isNaN(end.getTime()) || end <= start) errors.endTime = "End time must be after start time.";
  return errors;
}

function isCalendarEvent(value: unknown): value is CalendarEvent {
  if (!value || typeof value !== "object") return false;
  const event = value as Partial<CalendarEvent>;
  const start = new Date(event.startTime ?? "");
  const end = new Date(event.endTime ?? "");
  return typeof event.id === "string" && event.id.length > 0
    && typeof event.title === "string" && event.title.trim().length > 0
    && !Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime()) && end > start
    && typeof event.busy === "boolean"
    && (event.source == null || ["MANUAL", "CALDAV", "GOOGLE", "MICROSOFT"].includes(event.source));
}
