package com.back.suggestion;

import com.back.suggestion.entity.Suggestion;
import com.back.suggestion.repository.SuggestionRepository;
import com.back.suggestion.service.SuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "storage.upload-dir=build/test-uploads",
        "scheduling.worker.delay=60000",
        "scheduling.ttl.delay=60000"
})
class SuggestionServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("devtoolbox")
            .withUsername("devtoolbox")
            .withPassword("1234");

    @Autowired
    SuggestionService suggestionService;

    @Autowired
    SuggestionRepository suggestionRepository;

    @BeforeEach
    void cleanUp() {
        suggestionRepository.deleteAll();
    }

    @Test
    void addSuggestion_savesAndReturns() {
        Suggestion suggestion = suggestionService.addSuggestion("PDF 분할 기능에 페이지 범위 지정을 추가해주세요.");

        assertThat(suggestion.getId()).isNotNull();
        assertThat(suggestion.getContent()).isEqualTo("PDF 분할 기능에 페이지 범위 지정을 추가해주세요.");
        assertThat(suggestion.getCreatedAt()).isNotNull();
    }

    @Test
    void addSuggestion_persistsToDatabase() {
        suggestionService.addSuggestion("건의사항 내용");

        assertThat(suggestionRepository.findAll()).hasSize(1);
    }
}
