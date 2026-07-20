package com.oneil.wellness.walkplanner.calendar.persistence;
import jakarta.persistence.*; import java.io.Serializable; import java.time.Instant; import java.util.*;
@Entity @Table(name="provider_encrypted_credential") @IdClass(ProviderEncryptedCredential.Key.class)
public class ProviderEncryptedCredential {
 @Id @Column(name="provider_connection_id") private UUID providerConnectionId; @Id @Column(name="credential_key") private String credentialKey; @Column(name="credential_version") private int credentialVersion; @Column(nullable=false) private byte[] nonce; @Column(nullable=false) private byte[] ciphertext; @Column(name="updated_at") private Instant updatedAt;
 protected ProviderEncryptedCredential(){} public ProviderEncryptedCredential(UUID id,String key,int version,byte[] nonce,byte[] ciphertext){providerConnectionId=id;credentialKey=key;update(version,nonce,ciphertext);} public int getCredentialVersion(){return credentialVersion;} public byte[] getNonce(){return nonce.clone();} public byte[] getCiphertext(){return ciphertext.clone();} public void update(int version,byte[] newNonce,byte[] value){credentialVersion=version;nonce=newNonce.clone();ciphertext=value.clone();updatedAt=Instant.now();}
 public record Key(UUID providerConnectionId,String credentialKey) implements Serializable{}
}
