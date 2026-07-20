package com.oneil.wellness.walkplanner.calendar.persistence;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProviderCredentialReferenceRepository extends JpaRepository<ProviderCredentialReference,ProviderCredentialReference.Key>{long deleteByProviderConnectionId(UUID id);}
