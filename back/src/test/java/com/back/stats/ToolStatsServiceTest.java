package com.back.stats;

import com.back.AbstractMySQLIntegrationTest;
import com.back.stats.entity.ToolStats;
import com.back.stats.repository.ToolStatsRepository;
import com.back.stats.service.ToolStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "storage.upload-dir=build/test-uploads",
        "scheduling.worker.delay=60000",
        "scheduling.ttl.delay=60000"
})
class ToolStatsServiceTest extends AbstractMySQLIntegrationTest {

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
        toolStatsService.incrementUseCount("sql-formatter");

        ToolStats stats = toolStatsService.getOrCreate("sql-formatter");

        assertThat(toolStatsRepository.findAll()).hasSize(1);
        assertThat(stats.getUseCount()).isEqualTo(1);
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
        toolStatsService.incrementLikeCount("sql-formatter");

        ToolStats stats = toolStatsService.getOrCreate("sql-formatter");
        assertThat(stats.getLikeCount()).isEqualTo(2);
        assertThat(stats.getUseCount()).isZero();
    }

    @Test
    void decrementLikeCount_decreasesOnlyTargetModule() {
        toolStatsService.incrementLikeCount("sql-formatter");
        toolStatsService.incrementLikeCount("sql-formatter");
        toolStatsService.incrementLikeCount("cron"); // 다른 모듈은 영향받지 않아야 한다

        toolStatsService.decrementLikeCount("sql-formatter");

        assertThat(toolStatsService.getOrCreate("sql-formatter").getLikeCount()).isEqualTo(1);
        assertThat(toolStatsService.getOrCreate("cron").getLikeCount()).isEqualTo(1);
    }

    @Test
    void decrementLikeCount_neverGoesBelowZero() {
        toolStatsService.incrementLikeCount("sql-formatter");

        toolStatsService.decrementLikeCount("sql-formatter");
        toolStatsService.decrementLikeCount("sql-formatter");
        toolStatsService.decrementLikeCount("sql-formatter");

        assertThat(toolStatsService.getOrCreate("sql-formatter").getLikeCount()).isZero();
    }

    @Test
    void incrementUseCount_concurrentRequests_noLostUpdate() throws Exception {
        int threads = 20;
        var failures = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(threads)) {
            var latch = new java.util.concurrent.CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        toolStatsService.incrementUseCount("sql-formatter");
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertThat(failures).isEmpty();
        assertThat(toolStatsService.getOrCreate("sql-formatter").getUseCount()).isEqualTo(threads);
    }
}
