package com.back.suggestion.controller;

import com.back.suggestion.entity.Suggestion;
import com.back.suggestion.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> addSuggestion(@RequestBody Map<String, String> body) {
        Suggestion suggestion = suggestionService.addSuggestion(body.get("content"));
        return Map.of("id", suggestion.getId());
    }
}
