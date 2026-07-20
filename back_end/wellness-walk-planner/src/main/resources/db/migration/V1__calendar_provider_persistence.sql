CREATE TABLE calendar_provider_connection (
    id UUID PRIMARY KEY,
    provider_type VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    enabled BOOLEAN NOT NULL,
    connection_status VARCHAR(40) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    configuration_version INTEGER NOT NULL DEFAULT 1,
    server_url VARCHAR(2048),
    calendar_path VARCHAR(2048),
    default_timezone VARCHAR(100),
    last_successful_sync_at TIMESTAMP WITH TIME ZONE,
    last_attempted_sync_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE calendar_selection (
    id UUID PRIMARY KEY,
    provider_connection_id UUID NOT NULL REFERENCES calendar_provider_connection(id) ON DELETE CASCADE,
    provider_calendar_id VARCHAR(1024) NOT NULL,
    provider_calendar_name VARCHAR(512) NOT NULL,
    selected BOOLEAN NOT NULL,
    read_only BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_calendar_selection UNIQUE (provider_connection_id, provider_calendar_id)
);
CREATE INDEX idx_calendar_selection_connection_selected ON calendar_selection(provider_connection_id, selected);

CREATE TABLE provider_credential_reference (
    provider_connection_id UUID NOT NULL REFERENCES calendar_provider_connection(id) ON DELETE CASCADE,
    credential_key VARCHAR(120) NOT NULL,
    credential_version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (provider_connection_id, credential_key)
);

CREATE TABLE provider_encrypted_credential (
    provider_connection_id UUID NOT NULL,
    credential_key VARCHAR(120) NOT NULL,
    credential_version INTEGER NOT NULL,
    nonce BYTEA NOT NULL,
    ciphertext BYTEA NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (provider_connection_id, credential_key),
    CONSTRAINT fk_encrypted_credential_reference FOREIGN KEY (provider_connection_id, credential_key)
      REFERENCES provider_credential_reference(provider_connection_id, credential_key) ON DELETE CASCADE
);

CREATE TABLE oauth_authorization_state (
    state_hash VARCHAR(64) PRIMARY KEY,
    provider_type VARCHAR(32) NOT NULL,
    code_verifier_ciphertext TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    redirect_target VARCHAR(2048) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_oauth_state_provider_expiry ON oauth_authorization_state(provider_type, expires_at, used);

CREATE TABLE oauth_token_metadata (
    provider_connection_id UUID PRIMARY KEY REFERENCES calendar_provider_connection(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE,
    scopes VARCHAR(2048) NOT NULL,
    token_type VARCHAR(40) NOT NULL,
    last_refresh_at TIMESTAMP WITH TIME ZONE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
