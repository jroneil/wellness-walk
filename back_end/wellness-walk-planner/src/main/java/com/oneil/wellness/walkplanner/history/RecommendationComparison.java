package com.oneil.wellness.walkplanner.history;
import java.util.List;
public record RecommendationComparison(RecommendationSnapshot previous,RecommendationSnapshot current,int scoreDifference,List<String> significantReasons){}
