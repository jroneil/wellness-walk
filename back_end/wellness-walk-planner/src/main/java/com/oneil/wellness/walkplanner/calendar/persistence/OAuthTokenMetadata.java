package com.oneil.wellness.walkplanner.calendar.persistence;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="oauth_token_metadata")
public class OAuthTokenMetadata {
 @Id @Column(name="provider_connection_id") private UUID providerConnectionId; @Column(name="expires_at") private Instant expiresAt; private String scopes; @Column(name="token_type") private String tokenType; @Column(name="last_refresh_at") private Instant lastRefreshAt; private boolean revoked; @Column(name="created_at") private Instant createdAt; @Column(name="updated_at") private Instant updatedAt;
 protected OAuthTokenMetadata(){} public OAuthTokenMetadata(UUID id,Instant expiry,String scopes,String type){providerConnectionId=id;createdAt=Instant.now();update(expiry,scopes,type,false);} public Instant getExpiresAt(){return expiresAt;} public String getScopes(){return scopes;} public boolean isRevoked(){return revoked;} public void update(Instant expiry,String scopes,String type,boolean refresh){expiresAt=expiry;this.scopes=scopes;tokenType=type;if(refresh)lastRefreshAt=Instant.now();revoked=false;updatedAt=Instant.now();} public void revoke(){revoked=true;updatedAt=Instant.now();}
}
