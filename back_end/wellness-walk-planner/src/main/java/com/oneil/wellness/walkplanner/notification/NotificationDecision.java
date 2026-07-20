package com.oneil.wellness.walkplanner.notification;
import java.time.Instant;
public record NotificationDecision(boolean eligible,Instant scheduleAt,String reason,String title,String body){}
