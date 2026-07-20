"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import type { UserSettings } from "../types";
import { defaultUserSettings, loadSettings, resetSettings, saveSettings, validateSettings } from "./settings";

export default function SettingsPage() {
  const [settings, setSettings] = useState<UserSettings>(defaultUserSettings);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setSettings(loadSettings());
  }, []);

  function update<K extends keyof UserSettings>(key: K, value: UserSettings[K]) {
    setSettings((current) => ({ ...current, [key]: value }));
    setSaved(false);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validateSettings(settings);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }
    saveSettings(settings);
    setSettings(loadSettings());
    setSaved(true);
  }

  function handleReset() {
    resetSettings();
    setSettings(defaultUserSettings);
    setErrors({});
    setSaved(false);
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-3xl">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">Wellness Window</p>
            <h1 className="mt-2 text-3xl font-semibold">Settings</h1>
          </div>
          <Link
            href="/"
            className="rounded-2xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
          >
            ← Dashboard
          </Link>
        </div>
        <div className="mb-6 flex flex-wrap gap-3"><Link href="/settings/notifications" className="inline-flex rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-800">Notification settings →</Link><Link href="/settings/data" className="inline-flex rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-800">Goals &amp; data →</Link></div>

        <form noValidate onSubmit={handleSubmit} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="grid gap-5 sm:grid-cols-2">
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Default ZIP Code
              <input
                aria-invalid={Boolean(errors.defaultZipCode)}
                value={settings.defaultZipCode}
                onChange={(event) => update("defaultZipCode", event.target.value)}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
                inputMode="numeric"
                maxLength={5}
              />
              {errors.defaultZipCode ? <span className="text-sm text-rose-700">{errors.defaultZipCode}</span> : null}
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Preferred walk duration
              <select
                value={settings.walkDurationMinutes}
                onChange={(event) => update("walkDurationMinutes", Number(event.target.value) as UserSettings["walkDurationMinutes"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                {[10, 15, 20, 30, 45, 60].map((duration) => (
                  <option key={duration} value={duration}>
                    {duration} minutes
                  </option>
                ))}
              </select>
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Preferred Time
              <select
                value={settings.preferredTimeOfDay}
                onChange={(event) => update("preferredTimeOfDay", event.target.value as UserSettings["preferredTimeOfDay"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                <option value="ANY">Any time</option>
                <option value="MORNING">Morning</option>
                <option value="LUNCH">Lunch</option>
                <option value="AFTERNOON">Afternoon</option>
                <option value="EVENING">Evening</option>
              </select>
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Temperature Preference
              <select
                value={settings.temperaturePreference}
                onChange={(event) => update("temperaturePreference", event.target.value as UserSettings["temperaturePreference"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                <option value="COOLER">Cooler</option>
                <option value="BALANCED">Balanced</option>
                <option value="WARMER">Warmer</option>
              </select>
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Rain Tolerance
              <select
                value={settings.rainTolerance}
                onChange={(event) => update("rainTolerance", event.target.value as UserSettings["rainTolerance"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                <option value="AVOID_RAIN">Avoid rain</option>
                <option value="LIGHT_RAIN_OK">Light rain ok</option>
                <option value="RAIN_OK">Rain ok</option>
              </select>
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Wind Tolerance
              <select
                value={settings.windTolerance}
                onChange={(event) => update("windTolerance", event.target.value as UserSettings["windTolerance"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MODERATE">Moderate</option>
                <option value="HIGH">High</option>
              </select>
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Minimum acceptable recommendation score
              <input
                aria-invalid={Boolean(errors.minimumScore)}
                type="number"
                min={0}
                max={100}
                value={settings.minimumScore}
                onChange={(event) => update("minimumScore", Number(event.target.value))}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              />
              {errors.minimumScore ? <span className="text-sm text-rose-700">{errors.minimumScore}</span> : null}
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              Measurement units
              <select
                value={settings.unitSystem}
                onChange={(event) => update("unitSystem", event.target.value as UserSettings["unitSystem"])}
                className="rounded-2xl border border-slate-300 bg-slate-50 px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white"
              >
                <option value="US">US</option>
                <option value="METRIC">Metric</option>
              </select>
            </label>
          </div>

          {saved ? <p className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">Settings saved.</p> : null}

          <div className="mt-6 flex flex-wrap gap-3">
            <button type="submit" className="rounded-2xl bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800">
              Save settings
            </button>
            <button type="button" onClick={handleReset} className="rounded-2xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm">
              Reset defaults
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
