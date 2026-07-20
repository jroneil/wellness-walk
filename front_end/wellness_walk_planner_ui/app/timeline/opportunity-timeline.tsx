"use client";

import { useState } from "react";
import type { CalendarEvent, WeatherResponse } from "../types";
import { beginWalk, classifyOpportunity } from "../wellness-actions";

type LocalOutcome = "ACTIVE" | "SKIPPED" | "DISMISSED";
type TimelineEntry = {
  id: string;
  start: Date;
  end: Date;
  time: string;
  label: string;
  score: number | null;
  blocked: boolean;
  eligible: boolean;
};

const groups = ["Morning", "Afternoon", "Evening", "Overnight"] as const;

export function OpportunityTimeline({ weather, events }: { weather: WeatherResponse; events: CalendarEvent[] }) {
  const [outcomes, setOutcomes] = useState<Record<string, LocalOutcome>>({});
  const [pending, setPending] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [expanded, setExpanded] = useState(false);

  const entries: TimelineEntry[] = weather.hourlyForecast.slice(0, 12).map(hour => {
    const start = new Date(hour.startTime);
    const end = new Date(start.getTime() + 3_600_000);
    const conflict = events.find(event => event.busy && new Date(event.startTime) < end && new Date(event.endTime) > start);
    const score = hour.walkingRecommendation.score;
    return {
      id: hour.startTime,
      start,
      end,
      time: start.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }),
      label: conflict ? conflict.title : score != null && score >= 85 ? "Excellent" : score != null && score >= 60 ? "Good" : "Weather conflict",
      score,
      blocked: Boolean(conflict),
      eligible: !conflict && score != null && score >= 60 && end > new Date(),
    };
  });

  const relevant = entries.filter(entry => entry.start.getHours() >= 6 && entry.start.getHours() < 22);
  const overnight = entries.filter(entry => entry.start.getHours() < 6 || entry.start.getHours() >= 22);
  const defaultEntries = [...relevant, ...overnight].slice(0, 8);
  const visibleEntries = expanded ? entries : defaultEntries;

  async function act(id: string, start: Date, end: Date, action: "start" | "skip" | "dismiss") {
    setPending(id);
    setMessage("");
    try {
      if (action === "start") {
        await beginWalk(start.toISOString(), end.toISOString());
        setOutcomes(value => ({ ...value, [id]: "ACTIVE" }));
      } else {
        await classifyOpportunity(start.toISOString(), action, "TIMELINE");
        setOutcomes(value => ({ ...value, [id]: action === "skip" ? "SKIPPED" : "DISMISSED" }));
      }
      setMessage(action === "start" ? "Walk started." : action === "skip" ? "Opportunity marked as skipped." : "Suggestion dismissed.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "The timeline action could not be saved.");
    } finally {
      setPending(null);
    }
  }

  return <section className="mt-6 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><h3 className="text-xl font-semibold">Recommendation Timeline</h3><p className="mt-1 text-sm text-slate-600">The next relevant hours, grouped for faster scanning.</p></div>
      {entries.length > defaultEntries.length ? <button type="button" aria-expanded={expanded} onClick={() => setExpanded(value => !value)} className="rounded-xl border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500">{expanded ? "Show fewer hours" : `Show all ${entries.length} hours`}</button> : null}
    </div>
    {message ? <p role="status" className="mt-3 rounded-xl bg-slate-100 px-3 py-2 text-sm">{message}</p> : null}
    <div className="mt-4 space-y-4">
      {groups.map(group => {
        const grouped = visibleEntries.filter(entry => period(entry.start) === group);
        if (!grouped.length) return null;
        return <section key={group} aria-labelledby={`timeline-${group.toLowerCase()}`}>
          <h4 id={`timeline-${group.toLowerCase()}`} className="mb-2 text-sm font-semibold uppercase tracking-[0.12em] text-slate-500">{group}</h4>
          <ol aria-label={`${group} recommendation opportunities`} className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {grouped.map(entry => {
              const outcome = outcomes[entry.id];
              return <li key={entry.id} className={`rounded-2xl border-l-4 px-3 py-3 ${entry.blocked ? "border-rose-500 bg-rose-50" : entry.score != null && entry.score >= 85 ? "border-emerald-600 bg-emerald-50" : "border-amber-500 bg-amber-50"}`}>
                <div className="flex items-start justify-between gap-2"><p className="font-semibold">{entry.time}</p>{entry.score != null && !entry.blocked ? <strong className="text-lg">{entry.score}</strong> : null}</div>
                <p className="mt-0.5 text-sm text-slate-700">{entry.label}</p>
                {outcome ? <p className="mt-2 text-sm font-semibold">Outcome: {outcome === "ACTIVE" ? "Active walk" : outcome === "SKIPPED" ? "Skipped" : "Dismissed"}</p> : entry.eligible ? <div className="mt-2 flex flex-wrap gap-1.5" aria-label={`Actions for ${entry.time}`}>
                  <button disabled={pending !== null} onClick={() => act(entry.id, entry.start, entry.end, "start")} className="rounded-lg bg-emerald-700 px-2.5 py-1.5 text-sm font-semibold text-white disabled:opacity-50">Start walk</button>
                  <button disabled={pending !== null} onClick={() => act(entry.id, entry.start, entry.end, "skip")} className="rounded-lg border px-2.5 py-1.5 text-sm disabled:opacity-50">Skip</button>
                  <button disabled={pending !== null} onClick={() => act(entry.id, entry.start, entry.end, "dismiss")} className="rounded-lg border px-2.5 py-1.5 text-sm disabled:opacity-50">Dismiss</button>
                </div> : <p className="mt-1 text-xs font-medium text-slate-600">Outcome: Unknown</p>}
              </li>;
            })}
          </ol>
        </section>;
      })}
    </div>
  </section>;
}

function period(value: Date): typeof groups[number] {
  const hour = value.getHours();
  if (hour >= 6 && hour < 12) return "Morning";
  if (hour >= 12 && hour < 17) return "Afternoon";
  if (hour >= 17 && hour < 22) return "Evening";
  return "Overnight";
}
