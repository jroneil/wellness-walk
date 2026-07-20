import type { BackendStatusResponse } from "./types";

interface StatusSectionProps {
  status: BackendStatusResponse | null;
  error: string | null;
}

export default function StatusSection({ status, error }: StatusSectionProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-slate-50 p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-900">System status</h2>
          <p className="mt-1 text-sm text-slate-600">Backend connection check</p>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-sm font-medium ${
            error ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700"
          }`}
        >
          {error ? "Unavailable" : status?.status ?? "Unknown"}
        </span>
      </div>

      <div className="mt-4 flex flex-wrap gap-3 text-sm text-slate-600">
        <span>Reachable: {error ? "No" : "Yes"}</span>
        <span>Application: {status?.applicationName ?? "Waiting"}</span>
      </div>

      {error ? <p className="mt-3 text-sm text-rose-700">{error}</p> : null}
    </section>
  );
}
