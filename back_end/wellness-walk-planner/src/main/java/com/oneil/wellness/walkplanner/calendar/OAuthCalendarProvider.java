package com.oneil.wellness.walkplanner.calendar;
import com.oneil.wellness.walkplanner.calendar.model.*; import java.util.Set;
public interface OAuthCalendarProvider extends CalendarProvider {OAuthAuthorizationStart beginAuthorization();OAuthAuthorizationResult completeAuthorization(String code,String state,String error);void revokeAuthorization();Set<String> getGrantedScopes();}
