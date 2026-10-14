package com.myfinance.track.ai;

import com.myfinance.track.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    /** All fields optional: no dates means "this month". */
    public record SuggestionRequest(LocalDate from, LocalDate to, Boolean refresh) {}

    private final AiSuggestionService service;
    private final CurrentUser currentUser;

    public AiController(AiSuggestionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/suggestions")
    public InsightResponse suggest(@RequestBody(required = false) SuggestionRequest request) {
        SuggestionRequest r = request == null ? new SuggestionRequest(null, null, false) : request;
        return service.suggest(currentUser.id(), r.from(), r.to(), Boolean.TRUE.equals(r.refresh()));
    }

    /** No API call: whatever was stored last, or 204 if nothing yet. */
    @GetMapping("/suggestions/latest")
    public ResponseEntity<InsightResponse> latest() {
        return service.latest(currentUser.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}