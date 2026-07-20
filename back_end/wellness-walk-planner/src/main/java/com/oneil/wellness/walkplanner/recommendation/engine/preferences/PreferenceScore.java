package com.oneil.wellness.walkplanner.recommendation.engine.preferences;

import java.util.List;

public record PreferenceScore(int score, List<String> reasons) {}
