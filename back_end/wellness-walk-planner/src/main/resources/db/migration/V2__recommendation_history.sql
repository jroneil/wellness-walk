CREATE TABLE recommendation_history (
    id UUID PRIMARY KEY,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    score INTEGER NOT NULL,
    recommended_start TIMESTAMP WITH TIME ZONE NOT NULL,
    recommended_end TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DOUBLE PRECISION,
    wind DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    aqi DOUBLE PRECISION,
    uv DOUBLE PRECISION,
    calendar_available BOOLEAN NOT NULL,
    provider_sources VARCHAR(255) NOT NULL,
    reason_summary VARCHAR(1000) NOT NULL
);
CREATE INDEX idx_recommendation_history_captured_at ON recommendation_history(captured_at DESC);
