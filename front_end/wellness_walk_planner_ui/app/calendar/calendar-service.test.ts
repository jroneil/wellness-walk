// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from "vitest";
import { calendarStorageKey, deleteCalendarEvent, loadCalendarEvents, saveCalendarEvent } from "./calendar-service";

describe("calendar service", () => {
  beforeEach(() => window.localStorage.clear());

  it("persists, updates, and deletes manual events", () => {
    const event = { id: "one", title: "Standup", startTime: "2026-07-20T13:00:00.000Z", endTime: "2026-07-20T13:30:00.000Z", busy: true, source: "MANUAL" as const };
    saveCalendarEvent(event);
    saveCalendarEvent({ ...event, title: "Team standup", busy: false });

    expect(loadCalendarEvents()).toEqual([{ ...event, title: "Team standup", busy: false }]);
    expect(JSON.parse(window.localStorage.getItem(calendarStorageKey) ?? "[]")).toHaveLength(1);
    expect(deleteCalendarEvent("one")).toEqual([]);
  });

  it("ignores malformed stored events", () => {
    window.localStorage.setItem(calendarStorageKey, JSON.stringify([{ id: "bad", title: "", startTime: "nope" }]));
    expect(loadCalendarEvents()).toEqual([]);
  });
});
