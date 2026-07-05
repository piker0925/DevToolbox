package com.back.domain.job;

import com.back.global.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("local")
@TestPropertySource(properties = "storage.upload-dir=build/test-uploads")
class FileControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("devtoolbox")
            .withUsername("devtoolbox")
            .withPassword("1234");

    @Autowired WebApplicationContext wac;
    @Autowired FileStorage fileStorage;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void getFile_returnsFileContent() throws Exception {
        Path temp = Files.createTempFile("ctrl-test", ".txt");
        Files.writeString(temp, "hello-world");
        fileStorage.save("ctrl-test/result.txt", temp);

        mockMvc.perform(get("/api/v1/files/ctrl-test/result.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello-world"));
    }

    @Test
    void getFile_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/files/no-such/result.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiEndpoint_isPublicWithoutAuth() throws Exception {
        // 404(not 401/403) = security permitAll이 적용됨
        mockMvc.perform(get("/api/v1/files/no-auth-check/result.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsAllowedForLocalhost5173() throws Exception {
        mockMvc.perform(options("/api/v1/files/test")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
