package com.oneil.wellness.walkplanner.calendar.persistence;
import com.oneil.wellness.walkplanner.calendar.model.CalendarProviderType; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="oauth_authorization_state")
public class OAuthAuthorizationStateEntity {
 @Id @Column(name="state_hash") private String stateHash; @Enumerated(EnumType.STRING) @Column(name="provider_type") private CalendarProviderType providerType; @Column(name="code_verifier_ciphertext") private String codeVerifierCiphertext; @Column(name="created_at") private Instant createdAt; @Column(name="expires_at") private Instant expiresAt; @Column(name="redirect_target") private String redirectTarget; private boolean used;
 protected OAuthAuthorizationStateEntity(){} public OAuthAuthorizationStateEntity(String hash,CalendarProviderType type,String verifier,String redirect,Instant expiry){stateHash=hash;providerType=type;codeVerifierCiphertext=verifier;redirectTarget=redirect;createdAt=Instant.now();expiresAt=expiry;} public boolean isUsed(){return used;} public Instant getExpiresAt(){return expiresAt;} public String getCodeVerifierCiphertext(){return codeVerifierCiphertext;} public String getRedirectTarget(){return redirectTarget;} public CalendarProviderType getProviderType(){return providerType;} public void consume(){used=true;}
}
