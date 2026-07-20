package com.oneil.wellness.walkplanner.calendar.credential;
import java.util.*;
public interface ProviderCredentialStore {void saveSecret(UUID connectionId,String key,String value);Optional<String> readSecret(UUID connectionId,String key);void deleteSecret(UUID connectionId,String key);void deleteAll(UUID connectionId);void rotateSecret(UUID connectionId,String key,String value);boolean exists(UUID connectionId,String key);}
