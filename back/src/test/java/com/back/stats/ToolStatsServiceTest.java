package com.back.stats;

import com.back.stats.entity.ToolStats;
import com.back.stats.repository.ToolStatsRepository;
import com.back.stats.service.ToolStatsService;
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
class ToolStatsServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("devtoolbox")
            .withUsername("devtoolbox")
            .withPassword("1234");

    @Autowired
    ToolStatsService toolStatsService;

    @Autowired
    ToolStatsRepository toolStatsRepository;

    @BeforeEach
    void cleanUp() {
        toolStatsRepository.deleteAll();
    }

    @Test
    void getOrCreate_newModule_createsWithZeroCounts() {
        ToolStats stats = toolStatsService.getOrCreate("sql-formatter");

        assertThat(stats.getModuleId()).isEqualTo("sql-formatter");
        assertThat(stats.getUseCount()).isZero();
        assertThat(stats.getLikeCount()).isZero();
    }

    @Test
    void getOrCreate_existingModule_returnsSame() {
        toolStatsService.getOrCreate("sql-formatter");
        toolStatsService.getOrCreate("sql-formatter");

        assertThat(toolStatsRepository.findAll()).hasSize(1);
    }

    @Test
    void incrementUseCount_increasesCount() {
        toolStatsService.incrementUseCount("sql-formatter");
        toolStatsService.incrementUseCount("sql-formatter");

        ToolStats stats = toolStatsService.getOrCreate("sql-formatter");
        assertThat(stats.getUseCount()).isEqualTo(2);
    }

    @Test
    void incrementLikeCount_increasesCount() {
        toolStatsService.incrementLikeCount("sql-formatter");

        ToolStats stats = toolStatsService.getOrCreate("sql-formatter");
        assertThat(stats.getLikeCount()).isEqualTo(1);
    }
}
