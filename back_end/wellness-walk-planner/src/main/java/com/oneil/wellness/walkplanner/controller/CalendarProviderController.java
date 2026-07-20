package com.oneil.wellness.walkplanner.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.oneil.wellness.walkplanner.calendar.*;
import com.oneil.wellness.walkplanner.calendar.dto.*;
import com.oneil.wellness.walkplanner.calendar.model.*;
import com.oneil.wellness.walkplanner.calendar.service.CalendarSyncService;
import com.oneil.wellness.walkplanner.calendar.provider.caldav.CalDavException;
import com.oneil.wellness.walkplanner.calendar.provider.google.GoogleProviderException;
import java.net.URI;

@RestController
@RequestMapping("/api/calendar")
public class CalendarProviderController {
    private final CalendarProviderRegistry registry; private final CalendarSyncService syncService;
    public CalendarProviderController(CalendarProviderRegistry registry, CalendarSyncService syncService) { this.registry = registry; this.syncService = syncService; }
    @GetMapping("/providers") public List<CalendarProviderDto> providers() { return registry.list().stream().map(CalendarProviderDto::from).toList(); }
    @GetMapping("/providers/{type}/status") public CalendarProviderDto status(@PathVariable CalendarProviderType type) { return CalendarProviderDto.from(provider(type)); }
    @PostMapping("/providers/{type}/test") public CalendarConnectionResult test(@PathVariable CalendarProviderType type) { return provider(type).testConnection(); }
    @PostMapping("/providers/{type}/discover") public CalendarDiscoveryResult discover(@PathVariable CalendarProviderType type) { return provider(type).discoverCalendars(); }
    @GetMapping("/providers/{type}/calendars") public List<CalendarDescriptor> calendars(@PathVariable CalendarProviderType type) { return provider(type).listCalendars(); }
    @PutMapping("/providers/{type}/calendars/selection") public List<CalendarDescriptor> selection(@PathVariable CalendarProviderType type,@RequestBody CalendarSelectionRequest request) { return provider(type).selectCalendars(request == null ? List.of() : request.calendarIds()); }
    @GetMapping("/providers/{type}/sync-status") public CalendarProviderSyncStatus syncStatus(@PathVariable CalendarProviderType type){return provider(type).getSyncStatus();}
    @PostMapping("/providers/{type}/sync") public CalendarSyncResult providerSync(@PathVariable CalendarProviderType type,@RequestBody CalendarSyncRequest request){provider(type);return sync(request);}
    @PostMapping("/sync") public CalendarSyncResult sync(@RequestBody CalendarSyncRequest request) {
        if (request == null || request.start() == null || request.end() == null || !request.start().isBefore(request.end())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid bounded date range is required.");
        return syncService.synchronize(request.start(), request.end(), request.normalizedManualEvents());
    }
    @PostMapping("/providers/{type}/disconnect") public CalendarProviderDto disconnect(@PathVariable CalendarProviderType type) { CalendarProvider provider = provider(type); provider.disconnect(); return CalendarProviderDto.from(provider); }
    @GetMapping("/providers/{type}/oauth/start") public OAuthAuthorizationStart oauthStart(@PathVariable CalendarProviderType type) { if(provider(type) instanceof OAuthCalendarProvider oauth)return oauth.beginAuthorization();throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provider does not use OAuth."); }
    @GetMapping("/providers/{type}/oauth/callback") public ResponseEntity<Void> oauthCallback(@PathVariable CalendarProviderType type,@RequestParam(required=false)String code,@RequestParam(required=false)String state,@RequestParam(required=false)String error){if(!(provider(type) instanceof OAuthCalendarProvider oauth))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provider does not use OAuth.");OAuthAuthorizationResult result=oauth.completeAuthorization(code,state,error);return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(result.redirectUrl())).build();}
    @PostMapping("/providers/{type}/revoke") public CalendarProviderDto revoke(@PathVariable CalendarProviderType type){if(provider(type) instanceof OAuthCalendarProvider oauth)oauth.revokeAuthorization();else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provider does not use OAuth.");return CalendarProviderDto.from(provider(type));}
    private CalendarProvider provider(CalendarProviderType type) { return registry.find(type).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar provider is not registered.")); }
    @ExceptionHandler(CalDavException.class)
    public ResponseEntity<CalendarProviderErrorResponse> providerError(CalDavException error) {
        HttpStatus status=switch(error.getCode()){case "AUTHENTICATION_FAILED"->HttpStatus.UNAUTHORIZED;case "TIMEOUT"->HttpStatus.GATEWAY_TIMEOUT;case "UNKNOWN_CALENDAR","CALENDAR_SELECTION_REQUIRED","RANGE_LIMIT_EXCEEDED"->HttpStatus.BAD_REQUEST;default->HttpStatus.BAD_GATEWAY;};
        return ResponseEntity.status(status).body(new CalendarProviderErrorResponse(CalendarProviderType.CALDAV,error.getCode(),error.getMessage(),java.time.Instant.now()));
    }
    @ExceptionHandler(GoogleProviderException.class) public ResponseEntity<CalendarProviderErrorResponse> googleError(GoogleProviderException error){HttpStatus status=switch(error.getCode()){case "AUTHORIZATION_REQUIRED"->HttpStatus.UNAUTHORIZED;case "UNKNOWN_CALENDAR","INVALID_STATE","INSUFFICIENT_SCOPE"->HttpStatus.BAD_REQUEST;case "RATE_LIMITED"->HttpStatus.TOO_MANY_REQUESTS;case "TIMEOUT"->HttpStatus.GATEWAY_TIMEOUT;default->HttpStatus.BAD_GATEWAY;};return ResponseEntity.status(status).body(new CalendarProviderErrorResponse(CalendarProviderType.GOOGLE,error.getCode(),error.getMessage(),java.time.Instant.now()));}
}
