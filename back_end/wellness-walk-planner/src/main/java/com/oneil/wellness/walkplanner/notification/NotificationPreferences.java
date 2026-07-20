package com.oneil.wellness.walkplanner.notification;
import java.time.LocalTime;
public record NotificationPreferences(boolean enabled,int leadTimeMinutes,int minimumScore,boolean quietHoursEnabled,LocalTime quietHoursStart,LocalTime quietHoursEnd,boolean notifyOnWeekends,boolean workingHoursOnly,int maximumPerDay,int cooldownMinutes){public NotificationPreferences{leadTimeMinutes=Math.max(5,Math.min(30,leadTimeMinutes));minimumScore=Math.max(0,Math.min(100,minimumScore));maximumPerDay=Math.max(1,Math.min(20,maximumPerDay));cooldownMinutes=Math.max(5,Math.min(1440,cooldownMinutes));}}
