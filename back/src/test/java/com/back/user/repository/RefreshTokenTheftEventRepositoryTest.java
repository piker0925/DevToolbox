package com.back.user.repository;

import com.back.AbstractMySQLIntegrationTest;
import com.back.user.entity.RefreshTokenTheftEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RefreshTokenTheftEventRepositoryTest extends AbstractMySQLIntegrationTest {

    @Autowired
    RefreshTokenTheftEventRepository refreshTokenTheftEventRepository;

    private static final Long USER_A = 1001L;
    private static final Long USER_B = 1002L;

    @BeforeEach
    void cleanup() {
        refreshTokenTheftEventRepository.deleteAll();
    }

    @Test
    void save_userId_시각_ip를_그대로_저장하고_조회한다() {
        RefreshTokenTheftEvent saved = refreshTokenTheftEventRepository.save(
                new RefreshTokenTheftEvent(USER_A, LocalDateTime.now(), "203.0.113.9"));

        RefreshTokenTheftEvent reloaded = refreshTokenTheftEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(USER_A);
        assertThat(reloaded.getIp()).isEqualTo("203.0.113.9");
        assertThat(reloaded.getDetectedAt()).isNotNull();
    }

    @Test
    void countByUserId_해당_유저의_이벤트_수만_센다() {
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_A, LocalDateTime.now(), "1.1.1.1"));
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_A, LocalDateTime.now(), "2.2.2.2"));
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_B, LocalDateTime.now(), "3.3.3.3"));

        assertThat(refreshTokenTheftEventRepository.countByUserId(USER_A)).isEqualTo(2);
        assertThat(refreshTokenTheftEventRepository.countByUserId(USER_B)).isEqualTo(1);
    }

    @Test
    void countGroupedByUserIdIn_요청한_유저들의_발동횟수를_한번에_그룹핑한다() {
        Long userWithNoEvents = 1003L;
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_A, LocalDateTime.now(), "1.1.1.1"));
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_A, LocalDateTime.now(), "2.2.2.2"));
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(USER_B, LocalDateTime.now(), "3.3.3.3"));

        List<RefreshTokenTheftEventRepository.UserTheftCount> rows =
                refreshTokenTheftEventRepository.countGroupedByUserIdIn(List.of(USER_A, USER_B, userWithNoEvents));

        Map<Long, Long> byUserId = rows.stream()
                .collect(Collectors.toMap(RefreshTokenTheftEventRepository.UserTheftCount::getUserId,
                        RefreshTokenTheftEventRepository.UserTheftCount::getCount));

        assertThat(byUserId.get(USER_A)).isEqualTo(2L);
        assertThat(byUserId.get(USER_B)).isEqualTo(1L);
        assertThat(byUserId)
                .as("발동 이력이 없는 유저는 group by 결과에 행 자체가 없어야 한다")
                .doesNotContainKey(userWithNoEvents);
    }
}
