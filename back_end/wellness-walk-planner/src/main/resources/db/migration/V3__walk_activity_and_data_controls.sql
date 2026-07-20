CREATE TABLE walk_activity (
  id UUID PRIMARY KEY,
  recommendation_history_id UUID REFERENCES recommendation_history(id) ON DELETE SET NULL,
  opportunity_start TIMESTAMP WITH TIME ZONE NOT NULL,
  opportunity_end TIMESTAMP WITH TIME ZONE NOT NULL,
  activity_status VARCHAR(32) NOT NULL,
  actual_start TIMESTAMP WITH TIME ZONE,
  actual_end TIMESTAMP WITH TIME ZONE,
  duration_minutes INTEGER,
  perceived_quality VARCHAR(32),
  notes VARCHAR(1000),
  source VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT chk_walk_opportunity CHECK (opportunity_end > opportunity_start),
  CONSTRAINT chk_walk_actual CHECK (actual_end IS NULL OR actual_start IS NULL OR actual_end > actual_start),
  CONSTRAINT chk_walk_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0)
);
CREATE INDEX idx_walk_activity_start ON walk_activity(opportunity_start DESC);
CREATE INDEX idx_walk_activity_status ON walk_activity(activity_status);
CREATE INDEX idx_walk_activity_recommendation ON walk_activity(recommendation_history_id);

CREATE TABLE opportunity_outcome (
  id UUID PRIMARY KEY,
  recommendation_history_id UUID NOT NULL REFERENCES recommendation_history(id) ON DELETE CASCADE,
  outcome_status VARCHAR(24) NOT NULL,
  source VARCHAR(32) NOT NULL,
  recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(recommendation_history_id)
);
CREATE INDEX idx_opportunity_outcome_status ON opportunity_outcome(outcome_status, recorded_at);

CREATE TABLE wellness_goal_settings (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  enabled BOOLEAN NOT NULL,
  weekly_minutes_target INTEGER,
  weekly_walk_count_target INTEGER,
  minimum_qualifying_minutes INTEGER,
  week_starts_on VARCHAR(12) NOT NULL,
  timezone VARCHAR(80) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE history_retention_settings (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  recommendation_days INTEGER,
  notification_days INTEGER,
  expired_outcome_days INTEGER,
  activity_days INTEGER,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notification_delivery_history (
  id UUID PRIMARY KEY,
  delivered_at TIMESTAMP WITH TIME ZONE NOT NULL,
  recommendation_history_id UUID REFERENCES recommendation_history(id) ON DELETE SET NULL,
  delivery_status VARCHAR(24) NOT NULL
);
CREATE INDEX idx_notification_delivery_time ON notification_delivery_history(delivered_at);
