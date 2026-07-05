package com.back;

import com.back.job.repository.JobRepository;
import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolResult;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "storage.upload-dir=build/test-uploads",
        "scheduling.worker.delay=300",
        "scheduling.ttl.delay=60000"
})
@Import(ToolAndJobApiTest.TestModules.class)
class ToolAndJobApiTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("devtoolbox")
            .withUsername("devtoolbox")
            .withPassword("1234");

    @Autowired
    WebApplicationContext wac;
    @Autowired
    JobRepository jobRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jobRepository.deleteAll();
    }

    @Test
    void getModules_returnsList() throws Exception {
        mockMvc.perform(get("/api/v1/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == 'light-echo')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'heavy-echo')]").exists());
    }

    @Test
    void runLightTool_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/tools/light-echo/run")
                        .param("text", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("hello"));
    }

    @Test
    void uploadHeavyTool_singleFile_createsJobAndProcesses() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.txt", "text/plain", "data".getBytes());

        String resp = mockMvc.perform(multipart("/api/v1/tools/heavy-echo/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andReturn().getResponse().getContentAsString();

        String jobId = JsonPath.read(resp, "$.jobId");

        await().atMost(10, SECONDS).until(() -> {
            String statusResp = mockMvc.perform(get("/api/v1/jobs/" + jobId))
                    .andReturn().getResponse().getContentAsString();
            return "DONE".equals(JsonPath.read(statusResp, "$.status"));
        });

        mockMvc.perform(get("/api/v1/jobs/" + jobId + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("processed"));
    }

    @Test
    void uploadHeavyTool_multipleFiles_createsBatch() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.txt", "text/plain", "a".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "b.txt", "text/plain", "b".getBytes());

        String resp = mockMvc.perform(multipart("/api/v1/tools/heavy-echo/upload").file(file1).file(file2))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.jobIds").isArray())
                .andReturn().getResponse().getContentAsString();

        String batchId = JsonPath.read(resp, "$.batchId");

        await().atMost(15, SECONDS).until(() -> {
            String progressResp = mockMvc.perform(get("/api/v1/batches/" + batchId))
                    .andReturn().getResponse().getContentAsString();
            int done = JsonPath.read(progressResp, "$.doneCount");
            int total = JsonPath.read(progressResp, "$.totalCount");
            return done == total && total == 2;
        });

        mockMvc.perform(get("/api/v1/batches/" + batchId))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.doneCount").value(2))
                .andExpect(jsonPath("$.failCount").value(0));
    }

    @TestConfiguration
    static class TestModules {
        @Bean
        ToolModule lightEcho() {
            return new ToolModule() {
                public String getId() {
                    return "light-echo";
                }

                public String getName() {
                    return "Light Echo";
                }

                public String getCategory() {
                    return "test";
                }

                public boolean isHeavy() {
                    return false;
                }

                public ToolResult process(ToolInput input) {
                    return ToolResult.ofText(input.params().getOrDefault("text", "ok"));
                }
            };
        }

        @Bean
        ToolModule heavyEcho() {
            return new ToolModule() {
                public String getId() {
                    return "heavy-echo";
                }

                public String getName() {
                    return "Heavy Echo";
                }

                public String getCategory() {
                    return "test";
                }

                public boolean isHeavy() {
                    return true;
                }

                public ToolResult process(ToolInput input) {
                    return ToolResult.ofText("processed");
                }
            };
        }
    }
}
