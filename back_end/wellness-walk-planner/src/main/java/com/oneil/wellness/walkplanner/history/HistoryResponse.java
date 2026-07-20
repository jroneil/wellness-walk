package com.oneil.wellness.walkplanner.history;
import java.util.List;
public record HistoryResponse(List<RecommendationSnapshot> snapshots,HistorySummary today,HistorySummary week,RecommendationComparison comparison){}
