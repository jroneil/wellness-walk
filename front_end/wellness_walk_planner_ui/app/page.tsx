import StatusSection from "./status-section";
import WeatherSection from "./weather-section";
import { fetchBackendStatus } from "./lib/backend";
import Link from "next/link";

async function loadBackendStatus() {
  try {
    return {
      status: await fetchBackendStatus(),
      error: null,
    };
  } catch {
    return {
      status: null,
      error: "The backend is unavailable. Confirm it is running on the configured URL.",
    };
  }
}

export default async function Home() {
  const backendStatus = await loadBackendStatus();

  return (
    <main className="flex min-h-screen flex-col bg-[radial-gradient(circle_at_top,_rgba(16,185,129,0.08),_transparent_50%)] px-4 py-6 text-slate-900 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-5">
        <header className="rounded-3xl border border-slate-200 bg-white/80 p-5 shadow-sm backdrop-blur-sm sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">Wellness Window</p>
              <h1 className="mt-1 text-2xl font-semibold sm:text-3xl">
                Find a better moment to step away and recharge.
              </h1>
              <p className="mt-2 text-base text-slate-600">
                A calm weather-first view that makes the next few hours easier to read at a glance.
              </p>
            </div>
            <div className="flex w-full max-w-sm flex-col gap-3">
              <div className="flex justify-start gap-2 lg:justify-end">
                <Link href="/calendar" className="inline-flex h-10 items-center justify-center rounded-2xl border border-emerald-200 bg-white px-4 text-sm font-semibold text-emerald-800 shadow-sm hover:bg-emerald-50">
                  Calendar
                </Link>
                <Link href="/history" className="inline-flex h-10 items-center justify-center rounded-2xl border border-emerald-200 bg-white px-4 text-sm font-semibold text-emerald-800 shadow-sm hover:bg-emerald-50">History</Link>
                <Link href="/timeline" className="inline-flex h-10 items-center justify-center rounded-2xl border border-emerald-200 bg-white px-4 text-sm font-semibold text-emerald-800 shadow-sm hover:bg-emerald-50">Timeline</Link>
                <Link
                  href="/settings"
                  aria-label="Settings"
                  title="Settings"
                  className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-emerald-200 bg-white text-emerald-800 shadow-sm transition hover:border-emerald-300 hover:bg-emerald-50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
                >
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                    className="h-5 w-5"
                    fill="none"
                    stroke="currentColor"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                  >
                    <path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Z" />
                    <path d="M19.4 15a1.8 1.8 0 0 0 .36 1.98l.05.05a2.1 2.1 0 0 1-2.97 2.97l-.05-.05a1.8 1.8 0 0 0-1.98-.36 1.8 1.8 0 0 0-1.08 1.65V21.3a2.1 2.1 0 0 1-4.2 0v-.06a1.8 1.8 0 0 0-1.08-1.65 1.8 1.8 0 0 0-1.98.36l-.05.05a2.1 2.1 0 0 1-2.97-2.97l.05-.05A1.8 1.8 0 0 0 4.6 15a1.8 1.8 0 0 0-1.65-1.08H2.9a2.1 2.1 0 0 1 0-4.2h.06A1.8 1.8 0 0 0 4.6 8.64a1.8 1.8 0 0 0-.36-1.98l-.05-.05a2.1 2.1 0 0 1 2.97-2.97l.05.05a1.8 1.8 0 0 0 1.98.36 1.8 1.8 0 0 0 1.08-1.65V2.34a2.1 2.1 0 0 1 4.2 0v.06a1.8 1.8 0 0 0 1.08 1.65 1.8 1.8 0 0 0 1.98-.36l.05-.05a2.1 2.1 0 0 1 2.97 2.97l-.05.05a1.8 1.8 0 0 0-.36 1.98 1.8 1.8 0 0 0 1.65 1.08h.06a2.1 2.1 0 0 1 0 4.2h-.06A1.8 1.8 0 0 0 19.4 15Z" />
                  </svg>
                </Link>
              </div>
              <StatusSection status={backendStatus.status} error={backendStatus.error} />
            </div>
          </div>
        </header>

        <WeatherSection />
      </div>
    </main>
  );
}
