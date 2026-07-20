package com.oneil.wellness.walkplanner.calendar.persistence;
import jakarta.persistence.*; import java.io.Serializable; import java.time.Instant; import java.util.*;
@Entity @Table(name="provider_credential_reference") @IdClass(ProviderCredentialReference.Key.class)
public class ProviderCredentialReference {
 @Id @Column(name="provider_connection_id") private UUID providerConnectionId; @Id @Column(name="credential_key") private String credentialKey; @Column(name="credential_version") private int credentialVersion; @Column(name="created_at") private Instant createdAt; @Column(name="updated_at") private Instant updatedAt;
 protected ProviderCredentialReference(){} public ProviderCredentialReference(UUID id,String key,int version){providerConnectionId=id;credentialKey=key;credentialVersion=version;createdAt=updatedAt=Instant.now();} public int getCredentialVersion(){return credentialVersion;} public void rotate(){credentialVersion++;updatedAt=Instant.now();}
 public record Key(UUID providerConnectionId,String credentialKey) implements Serializable{}
}
