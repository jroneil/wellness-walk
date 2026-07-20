package com.oneil.wellness.walkplanner.history;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="recommendation_history")
public class RecommendationHistory {
    @Id private UUID id;
    @Column(name="captured_at",nullable=false) private Instant capturedAt;
    @Column(nullable=false) private int score;
    @Column(name="recommended_start",nullable=false) private OffsetDateTime recommendedStart;
    @Column(name="recommended_end",nullable=false) private OffsetDateTime recommendedEnd;
    private Double temperature; private Double wind; private Double humidity; private Double aqi; private Double uv;
    @Column(name="calendar_available",nullable=false) private boolean calendarAvailable;
    @Column(name="provider_sources",nullable=false,length=255) private String providerSources;
    @Column(name="reason_summary",nullable=false,length=1000) private String reasonSummary;
    protected RecommendationHistory(){}
    public RecommendationHistory(RecommendationSnapshot value){id=UUID.randomUUID();capturedAt=value.timestamp();score=value.score();recommendedStart=value.recommendedStart();recommendedEnd=value.recommendedEnd();temperature=value.temperature();wind=value.wind();humidity=value.humidity();aqi=value.aqi();uv=value.uv();calendarAvailable=value.calendarAvailable();providerSources=String.join(",",value.providerSources());reasonSummary=value.reasonSummary();}
    public RecommendationSnapshot snapshot(){return new RecommendationSnapshot(capturedAt,score,recommendedStart,recommendedEnd,temperature,wind,humidity,aqi,uv,calendarAvailable,providerSources.isBlank()?java.util.List.of():java.util.List.of(providerSources.split(",")),reasonSummary);}
    public UUID getId(){return id;} public Instant getCapturedAt(){return capturedAt;} public int getScore(){return score;} public OffsetDateTime getRecommendedStart(){return recommendedStart;} public OffsetDateTime getRecommendedEnd(){return recommendedEnd;} public boolean isCalendarAvailable(){return calendarAvailable;} public String getProviderSources(){return providerSources;} public String getReasonSummary(){return reasonSummary;}
}
