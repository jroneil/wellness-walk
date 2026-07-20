package com.oneil.wellness.walkplanner.notification;
import com.oneil.wellness.walkplanner.history.RecommendationSnapshot;import java.time.Instant;
public record NotificationEvaluationRequest(RecommendationSnapshot recommendation,NotificationPreferences preferences,Instant evaluatedAt,Instant lastNotificationAt,int notificationsToday,String timezone){}
