package com.back.user.service;

import com.back.AbstractMySQLIntegrationTest;
import com.back.user.entity.RefreshToken;
import com.back.user.entity.RefreshTokenTheftEvent;
import com.back.user.repository.RefreshTokenRepository;
import com.back.user.repository.RefreshTokenTheftEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 @Transactional 환경에서만 드러나는 문제(트랜잭션 롤백이 다른 삭제까지 되돌리는지)를 검증한다 —
// 순수 Mockito 유닛 테스트(RefreshTokenServiceTest)는 "메서드 호출 여부"만 보기 때문에 이 종류의
// 버그를 못 잡는다. 실제로 이 테스트가 처음 초록불이 되기 전, deleteAllByUserId가 rotate()의
// 트랜잭션과 함께 롤백돼 "전체 폐기"가 아무 일도 안 하는 버그를 잡아냈다.
@SpringBootTest
class RefreshTokenServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    RefreshTokenService refreshTokenService;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    RefreshTokenTheftEventRepository refreshTokenTheftEventRepository;

    private static final Long USER_ID = 12345L;

    @BeforeEach
    void cleanup() {
        refreshTokenRepository.deleteAll();
        refreshTokenTheftEventRepository.deleteAll();
    }

    @Test
    void 유예를_초과한_재사용은_빈값을_반환하면서도_해당_유저의_다른_토큰까지_실제로_삭제되어_남아있으면_안된다() {
        String survivorRawToken = refreshTokenService.issue(USER_ID).refreshToken();

        String rotatedRawToken = refreshTokenService.issue(USER_ID).refreshToken();
        refreshTokenService.rotate(rotatedRawToken, "127.0.0.1");

        RefreshToken rotatedRow = refreshTokenRepository.findByTokenHash(sha256(rotatedRawToken)).orElseThrow();
        rotatedRow.rotate(rotatedRow.getGraceToken(), LocalDateTime.now().minusSeconds(31));
        refreshTokenRepository.save(rotatedRow);

        assertThat(refreshTokenService.rotate(rotatedRawToken, "203.0.113.9")).isEmpty();

        assertThat(refreshTokenRepository.findByTokenHash(sha256(survivorRawToken)))
                .as("유예 초과 재사용은 탈취로 간주해 해당 유저의 다른 refresh token도 전부 폐기해야 한다")
                .isEmpty();
        assertThat(refreshTokenRepository.findById(rotatedRow.getId()))
                .as("반복 재생을 막기 위해 재사용된 토큰 자신도 지워야 한다")
                .isEmpty();
    }

    @Test
    void 유예를_초과한_재사용시_유저id_시각_ip가_theft_event_테이블에_기록된다() {
        String rotatedRawToken = refreshTokenService.issue(USER_ID).refreshToken();
        refreshTokenService.rotate(rotatedRawToken, "127.0.0.1");

        RefreshToken rotatedRow = refreshTokenRepository.findByTokenHash(sha256(rotatedRawToken)).orElseThrow();
        rotatedRow.rotate(rotatedRow.getGraceToken(), LocalDateTime.now().minusSeconds(31));
        refreshTokenRepository.save(rotatedRow);

        refreshTokenService.rotate(rotatedRawToken, "203.0.113.9");

        List<RefreshTokenTheftEvent> events = refreshTokenTheftEventRepository.findAll();
        assertThat(events).hasSize(1);
        RefreshTokenTheftEvent event = events.get(0);
        assertThat(event.getUserId()).isEqualTo(USER_ID);
        assertThat(event.getIp()).isEqualTo("203.0.113.9");
        assertThat(event.getDetectedAt()).isNotNull();
    }

    @Test
    void countTheftEventsByUserIds_발동_이력이_있는_유저와_없는_유저를_구분한다() {
        Long userWithTheft = 11111L;
        Long userWithoutTheft = 22222L;

        String rotatedRawToken = refreshTokenService.issue(userWithTheft).refreshToken();
        refreshTokenService.issue(userWithoutTheft); // 발동 없는 유저 — 영향 없어야 한다.
        refreshTokenService.rotate(rotatedRawToken, "127.0.0.1");
        RefreshToken rotatedRow = refreshTokenRepository.findByTokenHash(sha256(rotatedRawToken)).orElseThrow();
        rotatedRow.rotate(rotatedRow.getGraceToken(), LocalDateTime.now().minusSeconds(31));
        refreshTokenRepository.save(rotatedRow);
        refreshTokenService.rotate(rotatedRawToken, "203.0.113.9");

        Map<Long, Long> counts = refreshTokenService.countTheftEventsByUserIds(List.of(userWithTheft, userWithoutTheft));

        assertThat(counts.get(userWithTheft)).isEqualTo(1L);
        assertThat(counts.getOrDefault(userWithoutTheft, 0L))
                .as("발동 이력이 없는 유저는 카운트 맵에 잡히지 않고 호출부에서 0으로 처리되어야 한다")
                .isZero();
    }

    private static String sha256(String raw) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
