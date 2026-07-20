package com.oneil.wellness.walkplanner.controller;
import com.oneil.wellness.walkplanner.history.*;import com.oneil.wellness.walkplanner.notification.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/wellness") public class WellnessController{
 private final RecommendationHistoryService history;private final NotificationService notifications;public WellnessController(RecommendationHistoryService h,NotificationService n){history=h;notifications=n;}
 @PostMapping("/history") @ResponseStatus(HttpStatus.CREATED) public RecommendationSnapshot record(@RequestBody RecommendationSnapshot value){try{return history.record(value);}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}}
 @GetMapping("/history") public HistoryResponse history(){return history.history();}
 @PostMapping("/notifications/evaluate") public NotificationDecision evaluate(@RequestBody NotificationEvaluationRequest request){return notifications.evaluate(request);}
}
