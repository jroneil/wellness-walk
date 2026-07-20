package com.oneil.wellness.walkplanner.notification;
import java.time.Instant;import org.springframework.stereotype.Component;
@Component public class NotificationScheduler{public Instant scheduleAt(NotificationEvaluationRequest request){return request.recommendation().recommendedStart().toInstant().minusSeconds(request.preferences().leadTimeMinutes()*60L);}}
