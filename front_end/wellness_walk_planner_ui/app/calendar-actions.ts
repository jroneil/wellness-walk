"use server";

import type { CalendarEvent, CalendarProvider, CalendarProviderType } from "./types";
import { beginProviderAuthorization, disconnectCalendarProvider, discoverCalendarProvider, fetchCalendarProviderSnapshot, fetchProviderSyncStatus, revokeCalendarProvider, selectProviderCalendars, synchronizeCalendars, testCalendarProvider } from "./lib/backend";

const unavailable = "Calendar connections are temporarily unavailable. Manual events can still be used.";
const fallbackProvider = (type: CalendarProviderType): CalendarProvider => ({ type, displayName: type, installationConfigured:false, enabled: false, connected:false, authorizationRequired:false, selectedCalendarCount:0, discoveredCalendarCount:0, lastSuccessfulSyncAt:null, lastAttemptedSyncAt:null, providerStatus:"ERROR", safeMessage:unavailable, status: "ERROR", capabilities: { readEvents: false, writeEvents: false, discovery: false, recurrence: false } });

export async function getCalendarProviderSnapshot() { return fetchCalendarProviderSnapshot(); }
export async function testConnection(type: CalendarProviderType) { try{return await testCalendarProvider(type);}catch{return {status:"UNAVAILABLE" as const,message:unavailable};} }
export async function synchronizeCalendar(events: CalendarEvent[]) { try{return await synchronizeCalendars(events.filter((event) => event.source === "MANUAL"));}catch{return {events,errors:[{providerType:"CALDAV" as const,code:"UNAVAILABLE",message:unavailable}],calendars:[],synchronizedAt:new Date().toISOString(),partial:true};} }
export async function discoverCalendars() { try{return await discoverCalendarProvider("CALDAV");}catch{return {calendars:[],status:"NO_CALENDARS" as const,message:unavailable,discoveredAt:new Date().toISOString()};} }
export async function selectCalendars(calendarIds: string[]) { try{return await selectProviderCalendars("CALDAV", calendarIds);}catch{return [];} }
export async function getCalendarSyncStatus() { try{return await fetchProviderSyncStatus("CALDAV");}catch{return {lastAttempt:null,lastSuccess:null,status:"UNAVAILABLE",message:unavailable,calendars:[]};} }
export async function connectGoogleCalendar(){try{return await beginProviderAuthorization("GOOGLE");}catch{return {authorizationUrl:"",expiresAt:""};}}
export async function discoverGoogleCalendars(){try{return await discoverCalendarProvider("GOOGLE");}catch{return {calendars:[],status:"NO_CALENDARS" as const,message:unavailable,discoveredAt:new Date().toISOString()};}}
export async function selectGoogleCalendars(ids:string[]){try{return await selectProviderCalendars("GOOGLE",ids);}catch{return [];}}
export async function disconnectProvider(type:CalendarProviderType){try{return await disconnectCalendarProvider(type);}catch{return fallbackProvider(type);}}
export async function revokeProvider(type:CalendarProviderType){try{return await revokeCalendarProvider(type);}catch{return fallbackProvider(type);}}
