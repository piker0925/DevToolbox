package com.back.suggestion.service;

import com.back.suggestion.entity.Suggestion;
import com.back.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    @Transactional
    public Suggestion addSuggestion(String content) {
        Suggestion suggestion = new Suggestion();
        suggestion.setContent(content);
        return suggestionRepository.save(suggestion);
    }
}
