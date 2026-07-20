import { fetchCalendarProviderSnapshot } from "../lib/backend";
import { CalendarClient } from "./calendar-client";

export const dynamic = "force-dynamic";

export default async function CalendarPage() {
  const snapshot = await fetchCalendarProviderSnapshot();
  return <CalendarClient initialProviders={snapshot.providers} providersAvailable={snapshot.available} />;
}
