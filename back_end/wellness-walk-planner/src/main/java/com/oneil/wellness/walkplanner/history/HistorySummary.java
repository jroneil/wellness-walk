package com.oneil.wellness.walkplanner.history;
import java.time.LocalDate;import java.util.List;
public record HistorySummary(double averageRecommendation,Integer highestScore,String bestTime,int opportunities,int calendarConflicts,int weatherConflicts,int missedOpportunities,LocalDate bestDay,LocalDate mostBlockedDay,Double averageTemperature,Double averageAqi,Double averageUv,List<String> providerSources){}
