package com.oneil.wellness.walkplanner.calendar.persistence;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface OAuthTokenMetadataRepository extends JpaRepository<OAuthTokenMetadata,UUID>{}
