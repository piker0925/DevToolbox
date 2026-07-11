package com.back.admin;

import com.back.AbstractMySQLIntegrationTest;
import com.back.comment.entity.Comment;
import com.back.comment.repository.CommentRepository;
import com.back.stats.entity.ToolStats;
import com.back.stats.repository.ToolStatsRepository;
import com.back.suggestion.entity.Suggestion;
import com.back.suggestion.repository.SuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "storage.upload-dir=build/test-uploads",
        "scheduling.worker.delay=300",
        "scheduling.ttl.delay=60000"
})
class AdminControllerTest extends AbstractMySQLIntegrationTest {

    @Autowired
    WebApplicationContext wac;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    ToolStatsRepository toolStatsRepository;
    @Autowired
    SuggestionRepository suggestionRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getStats_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStats_withAuth_returns200() throws Exception {
        ToolStats stats = new ToolStats("sha256");
        stats.setUseCount(3);
        stats.setLikeCount(1);
        toolStatsRepository.save(stats);

        mockMvc.perform(get("/admin/stats")
                        .with(httpBasic("admin", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.moduleId == 'sha256')].useCount").value(3))
                .andExpect(jsonPath("$[?(@.moduleId == 'sha256')].likeCount").value(1));
    }

    @Test
    void getSuggestions_withAuth_returns200() throws Exception {
        Suggestion suggestion = new Suggestion();
        suggestion.setContent("more modules please");
        suggestionRepository.save(suggestion);

        mockMvc.perform(get("/admin/suggestions")
                        .with(httpBasic("admin", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.content == 'more modules please')]").exists());
    }

    @Test
    void deleteComment_withAuth_returns204() throws Exception {
        Comment comment = new Comment();
        comment.setModuleId("test-module");
        comment.setContent("test content");
        Comment saved = commentRepository.save(comment);

        mockMvc.perform(delete("/admin/comments/" + saved.getId())
                        .with(httpBasic("admin", "1234")))
                .andExpect(status().isNoContent());
    }

    @Test
    void publicApi_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/modules"))
                .andExpect(status().isOk());
    }
}
