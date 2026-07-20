import type { BackendStatusResponse, CalendarConnectionResult, CalendarDescriptor, CalendarDiscoveryResult, CalendarEvent, CalendarProvider, CalendarProviderSnapshot, CalendarProviderSyncStatus, CalendarProviderType, CalendarSyncResult, GoalProgress, HistoryResponse, HistoryRetention, NotificationDecision, OAuthAuthorizationStart, PerceivedQuality, RecommendationSnapshot, UserSettings, WalkActivity, WellnessGoal, WeatherResponse } from "../types";

const backendBaseUrl = process.env.BACKEND_URL ?? "http://localhost:9090";

async function jsonBody<T>(response: Response, fallback: string, allowEmpty = false): Promise<T> {
  const body = await response.text();
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) throw new Error("Calendar authorization is required.");
    if (response.status === 504) throw new Error("The calendar provider timed out.");
    if (body) {
      let value: { message?: unknown } | null = null;
      try { value = JSON.parse(body) as { message?: unknown }; } catch { value = null; }
      if (typeof value?.message === "string" && value.message.trim()) throw new Error(value.message);
    }
    throw new Error(fallback);
  }
  if (!body.trim()) {
    if (allowEmpty) return undefined as T;
    throw new Error(fallback);
  }
  try { return JSON.parse(body) as T; } catch { throw new Error(fallback); }
}

export async function fetchBackendStatus(): Promise<BackendStatusResponse> {
  const response = await fetch(`${backendBaseUrl}/api/health/status`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Backend request failed with status ${response.status}`);
  }

  return response.json();
}

export async function fetchCalendarProviders(): Promise<CalendarProvider[]> {
  const response = await fetch(`${backendBaseUrl}/api/calendar/providers`, { cache: "no-store" });
  return jsonBody<CalendarProvider[]>(response, "Calendar provider status is unavailable.");
}

export async function fetchCalendarProviderSnapshot(): Promise<CalendarProviderSnapshot> {
  try {
    const providers = await fetchCalendarProviders();
    return { providers: Array.isArray(providers) ? providers : [], available: true, message: null };
  } catch {
    return { providers: [], available: false, message: "Calendar connections are temporarily unavailable. Manual events can still be used." };
  }
}

export async function testCalendarProvider(type: CalendarProviderType): Promise<CalendarConnectionResult> {
  const response = await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/test`, { method: "POST", cache: "no-store" });
  return jsonBody<CalendarConnectionResult>(response, "Calendar connection test failed.");
}

export async function synchronizeCalendars(manualEvents: CalendarEvent[]): Promise<CalendarSyncResult> {
  const start = new Date(); const end = new Date(start.getTime() + 7 * 86_400_000);
  const response = await fetch(`${backendBaseUrl}/api/calendar/sync`, { method: "POST", cache: "no-store", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ start: start.toISOString(), end: end.toISOString(), manualEvents }) });
  return jsonBody<CalendarSyncResult>(response, "Calendar synchronization failed.");
}

export async function discoverCalendarProvider(type: CalendarProviderType): Promise<CalendarDiscoveryResult> {
  const response = await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/discover`, { method: "POST", cache: "no-store" });
  return jsonBody<CalendarDiscoveryResult>(response, "Calendar discovery failed.");
}

export async function selectProviderCalendars(type: CalendarProviderType, calendarIds: string[]): Promise<CalendarDescriptor[]> {
  const response = await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/calendars/selection`, { method: "PUT", cache: "no-store", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ calendarIds }) });
  return jsonBody<CalendarDescriptor[]>(response, "Calendar selection failed.");
}

export async function fetchProviderSyncStatus(type: CalendarProviderType): Promise<CalendarProviderSyncStatus> {
  const response = await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/sync-status`, { cache: "no-store" });
  return jsonBody<CalendarProviderSyncStatus>(response, "Calendar synchronization status is unavailable.");
}
export async function beginProviderAuthorization(type: CalendarProviderType): Promise<OAuthAuthorizationStart> { return jsonBody(await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/oauth/start`,{cache:"no-store"}),"Authorization could not be started."); }
export async function disconnectCalendarProvider(type: CalendarProviderType): Promise<CalendarProvider> { return jsonBody(await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/disconnect`,{method:"POST",cache:"no-store"}),"Calendar could not be disconnected."); }
export async function revokeCalendarProvider(type: CalendarProviderType): Promise<CalendarProvider> { return jsonBody(await fetch(`${backendBaseUrl}/api/calendar/providers/${type}/revoke`,{method:"POST",cache:"no-store"}),"Calendar authorization could not be revoked."); }
export async function recordRecommendationHistory(snapshot:RecommendationSnapshot):Promise<RecommendationSnapshot>{return jsonBody(await fetch(`${backendBaseUrl}/api/wellness/history`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify(snapshot)}),"Recommendation history could not be saved.");}
export async function fetchRecommendationHistory():Promise<HistoryResponse>{return jsonBody(await fetch(`${backendBaseUrl}/api/wellness/history`,{cache:"no-store"}),"Wellness history is unavailable.");}
export async function evaluateNotification(snapshot:RecommendationSnapshot,settings:UserSettings,lastNotificationAt:string|null,notificationsToday:number):Promise<NotificationDecision>{const preferences={enabled:settings.notificationsEnabled,leadTimeMinutes:settings.notificationLeadTimeMinutes,minimumScore:settings.minimumNotificationScore,quietHoursEnabled:settings.quietHoursEnabled,quietHoursStart:settings.quietHoursStart,quietHoursEnd:settings.quietHoursEnd,notifyOnWeekends:settings.notifyOnWeekends,workingHoursOnly:settings.workingHoursOnly,maximumPerDay:settings.maximumNotificationsPerDay,cooldownMinutes:settings.notificationCooldownMinutes};return jsonBody(await fetch(`${backendBaseUrl}/api/wellness/notifications/evaluate`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify({recommendation:snapshot,preferences,evaluatedAt:new Date().toISOString(),lastNotificationAt,notificationsToday,timezone:Intl.DateTimeFormat().resolvedOptions().timeZone})}),"Notification eligibility could not be evaluated.");}
export async function startWalk(start:string,end:string):Promise<WalkActivity>{return jsonBody(await fetch(`${backendBaseUrl}/api/walks/start`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify({opportunityStart:start,opportunityEnd:end,source:"DASHBOARD"})}),"Walk could not be started.");}
export async function activeWalk():Promise<WalkActivity|null>{const r=await fetch(`${backendBaseUrl}/api/walks/active`,{cache:"no-store"});if(r.status===204)return null;return jsonBody(r,"Active walk is unavailable.");}
export async function completeWalk(id:string,durationMinutes:number,partiallyCompleted:boolean,perceivedQuality:PerceivedQuality|null,notes:string):Promise<WalkActivity>{return jsonBody(await fetch(`${backendBaseUrl}/api/walks/${id}/complete`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify({durationMinutes,partiallyCompleted,perceivedQuality,notes})}),"Walk could not be completed.");}
export async function cancelWalk(id:string):Promise<void>{await jsonBody(await fetch(`${backendBaseUrl}/api/walks/${id}/cancel`,{method:"POST",cache:"no-store"}),"Walk could not be cancelled.",true);}
export async function listWalks():Promise<WalkActivity[]>{return jsonBody(await fetch(`${backendBaseUrl}/api/walks`,{cache:"no-store"}),"Walk activity is unavailable.");}
export async function manualWalk(actualStart:string,durationMinutes:number,perceivedQuality:PerceivedQuality|null,notes:string):Promise<WalkActivity>{return jsonBody(await fetch(`${backendBaseUrl}/api/walks/manual`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify({actualStart,durationMinutes,perceivedQuality,notes})}),"Manual walk could not be saved.");}
export async function fetchGoal():Promise<WellnessGoal>{return jsonBody(await fetch(`${backendBaseUrl}/api/settings/wellness-goals`,{cache:"no-store"}),"Wellness goal is unavailable.");}
export async function saveGoal(value:WellnessGoal):Promise<WellnessGoal>{return jsonBody(await fetch(`${backendBaseUrl}/api/settings/wellness-goals`,{method:"PUT",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify(value)}),"Wellness goal could not be saved.");}
export async function fetchGoalProgress():Promise<GoalProgress>{return jsonBody(await fetch(`${backendBaseUrl}/api/settings/wellness-goals/progress`,{cache:"no-store"}),"Goal progress is unavailable.");}
export async function fetchRetention():Promise<HistoryRetention>{return jsonBody(await fetch(`${backendBaseUrl}/api/settings/history-retention`,{cache:"no-store"}),"Retention settings are unavailable.");}
export async function saveRetention(value:HistoryRetention):Promise<HistoryRetention>{return jsonBody(await fetch(`${backendBaseUrl}/api/settings/history-retention`,{method:"PUT",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify(value)}),"Retention settings could not be saved.");}
export async function recordOpportunityOutcome(start:string,status:"skip"|"dismiss",source:"DASHBOARD"|"TIMELINE"|"NOTIFICATION"="DASHBOARD"){return jsonBody(await fetch(`${backendBaseUrl}/api/opportunities/${status}`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json"},body:JSON.stringify({opportunityStart:start,source})}),"Opportunity outcome could not be saved.");}
export async function deleteHistory(kind:"recommendations"|"notifications"|"activities"|"all",from?:string,to?:string){const path=kind==="all"?"all":kind;const url=new URL(`${backendBaseUrl}/api/history/${path}`);if(from&&to){url.searchParams.set("from",from);url.searchParams.set("to",to);url.searchParams.set("timezone",Intl.DateTimeFormat().resolvedOptions().timeZone)}return jsonBody(await fetch(url,{method:"DELETE",cache:"no-store"}),"History could not be deleted.");}

export async function fetchWeatherCurrent(zipCode: string, settings?: UserSettings, calendarEvents: CalendarEvent[] = []): Promise<WeatherResponse> {
  if (settings || calendarEvents.length > 0) {
    const response = await fetch(`${backendBaseUrl}/api/weather/current/${zipCode}/recommendation`, {
      method: "POST",
      cache: "no-store",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ preferences: settings, calendarEvents }),
    });
    if (!response.ok) throw new Error(weatherErrorMessage(response.status));
    return response.json();
  }
  const url = new URL(`${backendBaseUrl}/api/weather/current/${zipCode}`);

  const response = await fetch(url, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(weatherErrorMessage(response.status));
  }

  return response.json();
}

function weatherErrorMessage(status: number): string {
  if (status === 400) {
    return "Enter a valid 5-digit ZIP code.";
  }
  if (status === 404) {
    return "We could not find that ZIP code.";
  }
  if (status === 502) {
    return "Weather is unavailable for that location right now.";
  }
  if (status === 503) {
    return "The ZIP lookup service is temporarily unavailable.";
  }
  return "Weather request failed. Please try again.";
}
