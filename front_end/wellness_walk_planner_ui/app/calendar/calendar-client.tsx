"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import type { CalendarEvent } from "../types";
import { deleteCalendarEvent, loadCalendarEvents, saveCalendarEvent, validateCalendarEvent } from "./calendar-service";
import { CalendarConnections } from "./calendar-connections";

function emptyEvent(): CalendarEvent {
  const start = new Date();
  start.setMinutes(Math.ceil(start.getMinutes() / 30) * 30, 0, 0);
  const end = new Date(start.getTime() + 30 * 60_000);
  return { id: "", title: "", startTime: start.toISOString(), endTime: end.toISOString(), busy: true, source: "MANUAL" };
}

function inputValue(value: string): string {
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function readable(value: string): string {
  return new Intl.DateTimeFormat("en", { weekday: "short", month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(new Date(value));
}

export function CalendarClient({ initialProviders = [], providersAvailable = true }: { initialProviders?: import("../types").CalendarProvider[]; providersAvailable?: boolean }) {
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [draft, setDraft] = useState<CalendarEvent>(emptyEvent);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => setEvents(loadCalendarEvents()), []);

  function update<K extends keyof CalendarEvent>(key: K, value: CalendarEvent[K]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const candidate = { ...draft, id: draft.id || crypto.randomUUID(), source: "MANUAL" as const };
    const nextErrors = validateCalendarEvent(candidate);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;
    setEvents(saveCalendarEvent(candidate));
    setDraft(emptyEvent());
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div><p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">Wellness Window</p><h1 className="mt-2 text-3xl font-semibold">Calendar</h1></div>
          <Link href="/" className="rounded-2xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold shadow-sm">← Dashboard</Link>
        </header>

        <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <form noValidate onSubmit={submit} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-xl font-semibold">{draft.id ? "Edit event" : "Add event"}</h2>
            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-medium">Title<input value={draft.title} onChange={(event) => update("title", event.target.value)} className="rounded-2xl border border-slate-300 px-3 py-2" />{errors.title ? <span className="text-rose-700">{errors.title}</span> : null}</label>
              <label className="grid gap-2 text-sm font-medium">Start<input type="datetime-local" value={inputValue(draft.startTime)} onChange={(event) => update("startTime", new Date(event.target.value).toISOString())} className="rounded-2xl border border-slate-300 px-3 py-2" />{errors.startTime ? <span className="text-rose-700">{errors.startTime}</span> : null}</label>
              <label className="grid gap-2 text-sm font-medium">End<input type="datetime-local" value={inputValue(draft.endTime)} onChange={(event) => update("endTime", new Date(event.target.value).toISOString())} className="rounded-2xl border border-slate-300 px-3 py-2" />{errors.endTime ? <span className="text-rose-700">{errors.endTime}</span> : null}</label>
              <label className="flex items-center gap-3 text-sm font-medium"><input type="checkbox" checked={draft.busy} onChange={(event) => update("busy", event.target.checked)} /> Mark this event busy</label>
            </div>
            <div className="mt-6 flex gap-3"><button className="rounded-2xl bg-emerald-700 px-4 py-2 text-sm font-semibold text-white">{draft.id ? "Save changes" : "Add event"}</button>{draft.id ? <button type="button" onClick={() => setDraft(emptyEvent())} className="rounded-2xl border border-slate-300 px-4 py-2 text-sm font-semibold">Cancel</button> : null}</div>
          </form>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-xl font-semibold">Today&apos;s Schedule</h2>
            <p className="mt-1 text-sm text-slate-600">Busy events block recommendations; free events remain visible but do not block a walk.</p>
            {events.length ? <ol aria-label="Calendar events" className="mt-5 space-y-3">{events.map((event) => <li key={event.id} className={`rounded-2xl border-l-4 p-4 ${event.busy ? "border-rose-500 bg-rose-50" : "border-sky-500 bg-sky-50"}`}><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-semibold">{event.title}</p><p className="mt-1 text-sm text-slate-600">{readable(event.startTime)}–{readable(event.endTime)} · {event.busy ? "Busy" : "Free"} · {event.source === "CALDAV" ? `CalDAV${event.calendarId ? ` · ${event.calendarId}` : ""}` : "Manual"}</p></div>{event.source === "MANUAL" ? <div className="flex gap-2"><button type="button" onClick={() => setDraft(event)} className="rounded-xl border border-slate-300 bg-white px-3 py-1 text-sm font-semibold">Edit</button><button type="button" onClick={() => setEvents(deleteCalendarEvent(event.id))} className="rounded-xl border border-rose-300 bg-white px-3 py-1 text-sm font-semibold text-rose-700">Delete</button></div> : null}</div></li>)}</ol> : <p className="mt-5 rounded-2xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-600">No calendar events yet.</p>}
          </section>
        </div>
        <CalendarConnections initialProviders={initialProviders} providersAvailable={providersAvailable} manualEvents={events.filter((event) => event.source === "MANUAL")} onEvents={setEvents} />
      </div>
    </main>
  );
}
