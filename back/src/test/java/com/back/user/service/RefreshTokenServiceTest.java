package com.back.user.service;

import com.back.global.security.jwt.JwtProvider;
import com.back.user.dto.TokenPair;
import com.back.user.entity.RefreshToken;
import com.back.user.entity.RefreshTokenTheftEvent;
import com.back.user.repository.RefreshTokenRepository;
import com.back.user.repository.RefreshTokenTheftEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    RefreshTokenTheftEventRepository refreshTokenTheftEventRepository;
    @Mock
    JwtProvider jwtProvider;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, refreshTokenTheftEventRepository, jwtProvider, FIXED_CLOCK);
    }

    @Test
    void issue_새로운_refreshToken을_해시로_저장하고_토큰쌍을_반환한다() {
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token");

        TokenPair result = refreshTokenService.issue(1L);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTokenHash()).isEqualTo(sha256(result.refreshToken()));
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusDays(14));
    }

    @Test
    void rotate_존재하지_않는_토큰이면_빈값을_반환한다() {
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThat(refreshTokenService.rotate("garbage", "1.2.3.4")).isEmpty();
    }

    @Test
    void rotate_만료된_토큰이면_빈값을_반환한다() {
        String rawToken = "expired-raw";
        RefreshToken row = new RefreshToken(1L, sha256(rawToken), NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken))).thenReturn(Optional.of(row));

        assertThat(refreshTokenService.rotate(rawToken, "1.2.3.4")).isEmpty();
    }

    @Test
    void rotate_아직_회전되지_않은_토큰이면_새_토큰쌍을_발급하고_이전_토큰을_회전_기록한다() {
        String rawToken = "current-raw";
        RefreshToken row = new RefreshToken(1L, sha256(rawToken), NOW.plusDays(14));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken))).thenReturn(Optional.of(row));
        when(jwtProvider.issueAccessToken(1L)).thenReturn("new-access");

        TokenPair result = refreshTokenService.rotate(rawToken, "1.2.3.4").orElseThrow();

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(rawToken);

        assertThat(row.getRotatedAt()).isEqualTo(NOW);
        assertThat(row.getGraceToken()).isEqualTo(result.refreshToken());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken successor = captor.getAllValues().get(0);
        assertThat(successor.getUserId()).isEqualTo(1L);
        assertThat(successor.getTokenHash()).isEqualTo(sha256(result.refreshToken()));
    }

    @Test
    void rotate_회전_직후_30초_이내_재사용이면_동일한_후속_토큰쌍을_반환하고_탈취로_기록하지_않는다() {
        String oldRaw = "old-raw";
        RefreshToken row = new RefreshToken(1L, sha256(oldRaw), NOW.plusDays(14));
        row.rotate("successor-raw", NOW.minusSeconds(10));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(oldRaw))).thenReturn(Optional.of(row));
        when(jwtProvider.issueAccessToken(1L)).thenReturn("fresh-access");

        TokenPair result = refreshTokenService.rotate(oldRaw, "1.2.3.4").orElseThrow();

        assertThat(result.accessToken()).isEqualTo("fresh-access");
        assertThat(result.refreshToken()).isEqualTo("successor-raw");
        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        // 유예 이내 재사용은 오탐(멀티탭 레이스)일 수 있어 탈취 이벤트로 기록하지 않는다 —
        // 아래 유예 초과 테스트와 짝을 이뤄 "진짜 탈취일 때만 기록"을 확인한다.
        verifyNoInteractions(refreshTokenTheftEventRepository);
    }

    @Test
    void rotate_유예를_초과한_재사용은_탈취로_간주해_해당_유저의_토큰을_전부_폐기하고_이벤트를_기록한다() {
        String oldRaw = "old-raw";
        RefreshToken row = new RefreshToken(1L, sha256(oldRaw), NOW.plusDays(14));
        row.rotate("successor-raw", NOW.minusSeconds(31));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(oldRaw))).thenReturn(Optional.of(row));

        assertThat(refreshTokenService.rotate(oldRaw, "9.9.9.9")).isEmpty();

        // current 자신도 포함해 지운다 — 제외하면 같은 유출 토큰을 반복 재생해 계정을 계속 로그아웃시킬 수 있다.
        verify(refreshTokenRepository).deleteAllByUserId(1L);

        ArgumentCaptor<RefreshTokenTheftEvent> eventCaptor = ArgumentCaptor.forClass(RefreshTokenTheftEvent.class);
        verify(refreshTokenTheftEventRepository).save(eventCaptor.capture());
        RefreshTokenTheftEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getUserId()).isEqualTo(1L);
        assertThat(savedEvent.getIp()).isEqualTo("9.9.9.9");
        assertThat(savedEvent.getDetectedAt()).isEqualTo(NOW);
    }

    @Test
    void rotate_요청_IP가_비어있으면_unknown으로_기록한다() {
        String oldRaw = "old-raw";
        RefreshToken row = new RefreshToken(1L, sha256(oldRaw), NOW.plusDays(14));
        row.rotate("successor-raw", NOW.minusSeconds(31));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(oldRaw))).thenReturn(Optional.of(row));

        refreshTokenService.rotate(oldRaw, " ");

        ArgumentCaptor<RefreshTokenTheftEvent> eventCaptor = ArgumentCaptor.forClass(RefreshTokenTheftEvent.class);
        verify(refreshTokenTheftEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getIp()).isEqualTo("unknown");
    }

    @Test
    void revoke_본인의_토큰이면_삭제한다() {
        String rawToken = "raw";
        RefreshToken row = new RefreshToken(1L, sha256(rawToken), NOW.plusDays(14));
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(row));

        refreshTokenService.revoke(rawToken, 1L);

        verify(refreshTokenRepository).delete(row);
    }

    @Test
    void revoke_다른_유저의_토큰이면_삭제하지_않는다() {
        String rawToken = "raw";
        RefreshToken row = new RefreshToken(1L, sha256(rawToken), NOW.plusDays(14));
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(row));

        refreshTokenService.revoke(rawToken, 2L);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void revoke_존재하지_않는_토큰이면_예외없이_아무일도_하지_않는다() {
        when(refreshTokenRepository.findByTokenHash(sha256("does-not-exist"))).thenReturn(Optional.empty());

        refreshTokenService.revoke("does-not-exist", 1L);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void countTheftEventsByUserIds_빈_컬렉션이면_레포지토리를_호출하지_않고_빈_맵을_반환한다() {
        Map<Long, Long> result = refreshTokenService.countTheftEventsByUserIds(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(refreshTokenTheftEventRepository);
    }

    @Test
    void countTheftEventsByUserIds_유저별_발동_횟수를_맵으로_반환한다() {
        RefreshTokenTheftEventRepository.UserTheftCount rowFor1 = userTheftCount(1L, 3L);
        when(refreshTokenTheftEventRepository.countGroupedByUserIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(rowFor1));

        Map<Long, Long> result = refreshTokenService.countTheftEventsByUserIds(List.of(1L, 2L));

        // 발동 이력이 있는 유저(1L)는 실제 횟수가, 없는 유저(2L)는 결과 맵에 아예 안 잡히는 것을
        // 호출부(AdminController)가 getOrDefault(id, 0L)로 0 처리한다 — 두 유형을 구분해서 확인한다.
        assertThat(result).containsExactly(Map.entry(1L, 3L));
        assertThat(result.getOrDefault(2L, 0L)).isZero();
    }

    private static RefreshTokenTheftEventRepository.UserTheftCount userTheftCount(Long userId, Long count) {
        return new RefreshTokenTheftEventRepository.UserTheftCount() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
