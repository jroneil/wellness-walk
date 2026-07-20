package com.oneil.wellness.walkplanner.calendar.persistence;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType;
public interface OAuthAuthorizationStateRepository extends JpaRepository<OAuthAuthorizationStateEntity,String>{@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from OAuthAuthorizationStateEntity s where s.stateHash=:hash") java.util.Optional<OAuthAuthorizationStateEntity> findForUpdate(@Param("hash") String hash);}
