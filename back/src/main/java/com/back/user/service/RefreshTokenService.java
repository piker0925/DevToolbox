package com.back.user.service;

import com.back.global.security.TokenHasher;
import com.back.global.security.jwt.JwtProvider;
import com.back.user.dto.TokenPair;
import com.back.user.entity.RefreshToken;
import com.back.user.entity.RefreshTokenTheftEvent;
import com.back.user.repository.RefreshTokenRepository;
import com.back.user.repository.RefreshTokenTheftEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(14);
    // 회전 직후 동시 탭이 구 토큰으로 재요청해도 로그아웃되지 않도록 두는 유예(ADR-0024).
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UNKNOWN_IP = "unknown";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenTheftEventRepository refreshTokenTheftEventRepository;
    private final JwtProvider jwtProvider;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                RefreshTokenTheftEventRepository refreshTokenTheftEventRepository,
                                JwtProvider jwtProvider,
                                @Value("${jwt.refresh-token-expiration-days:14}") long refreshTokenExpirationDays) {
        this(refreshTokenRepository, refreshTokenTheftEventRepository, jwtProvider,
                Clock.systemDefaultZone(), Duration.ofDays(refreshTokenExpirationDays));
    }

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                         RefreshTokenTheftEventRepository refreshTokenTheftEventRepository,
                         JwtProvider jwtProvider, Clock clock) {
        this(refreshTokenRepository, refreshTokenTheftEventRepository, jwtProvider, clock, DEFAULT_REFRESH_TOKEN_TTL);
    }

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                         RefreshTokenTheftEventRepository refreshTokenTheftEventRepository,
                         JwtProvider jwtProvider, Clock clock, Duration refreshTokenTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTheftEventRepository = refreshTokenTheftEventRepository;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public TokenPair issue(Long userId) {
        String rawToken = generateRawToken();
        RefreshToken token = new RefreshToken(userId, TokenHasher.sha256(rawToken), now().plus(refreshTokenTtl));
        refreshTokenRepository.save(token);
        return new TokenPair(jwtProvider.issueAccessToken(userId), rawToken);
    }

    // 탈취 감지(유예 초과 재사용)를 포함해 어떤 경우든 예외를 던지지 않고 Optional.empty()를
    // 반환한다 — 여기서 던지면 이 메서드의 트랜잭션 전체(탈취 시 전체 폐기 포함)가 롤백되어
    // "폐기했다"는 게 실제로는 아무 일도 안 한 게 된다. 401 변환은 AuthController가 담당한다.
    @Transactional
    public Optional<TokenPair> rotate(String rawRefreshToken, String requestIp) {
        Optional<RefreshToken> maybeCurrent = refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.sha256(rawRefreshToken));
        if (maybeCurrent.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken current = maybeCurrent.get();
        if (current.getExpiresAt().isBefore(now())) {
            return Optional.empty();
        }

        if (current.getRotatedAt() == null) {
            return Optional.of(rotateFresh(current));
        }
        return reuseWithinGraceOrRevoke(current, requestIp);
    }

    @Transactional
    public void revoke(String rawRefreshToken, Long authenticatedUserId) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .filter(token -> token.getUserId().equals(authenticatedUserId))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void forceLogout(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private TokenPair rotateFresh(RefreshToken current) {
        Long userId = current.getUserId();
        String newRawToken = generateRawToken();
        RefreshToken successor = new RefreshToken(userId, TokenHasher.sha256(newRawToken), now().plus(refreshTokenTtl));
        refreshTokenRepository.save(successor);

        current.rotate(newRawToken, now());
        refreshTokenRepository.save(current);

        return new TokenPair(jwtProvider.issueAccessToken(userId), newRawToken);
    }

    private Optional<TokenPair> reuseWithinGraceOrRevoke(RefreshToken current, String requestIp) {
        Duration sinceRotation = Duration.between(current.getRotatedAt(), now());
        if (sinceRotation.compareTo(GRACE_PERIOD) <= 0) {
            return Optional.of(new TokenPair(jwtProvider.issueAccessToken(current.getUserId()), current.getGraceToken()));
        }
        // 탈취 감지 발동 기록(057) — 개별 이벤트만으로는 오탐(멀티탭 레이스)과 진짜 탈취를 구분할 수
        // 없어 관리자 화면에서는 유저별 발동 빈도로만 노출한다. rotate()의 트랜잭션과 같이 묶이므로
        // 이 insert가 실패하면 전체 폐기까지 롤백된다 — ip가 null/blank라도 NOT NULL 제약에 걸려
        // 예외가 나지 않도록 sanitize한다.
        refreshTokenTheftEventRepository.save(new RefreshTokenTheftEvent(current.getUserId(), now(), sanitizeIp(requestIp)));
        // current 자신도 포함해서 지운다 — 제외하면 같은 유출된 토큰을 반복 재생해 매번 계정을
        // 다시 로그아웃시키는 용도로 악용될 수 있다.
        refreshTokenRepository.deleteAllByUserId(current.getUserId());
        return Optional.empty();
    }

    // 관리자 유저 목록에서 유저별 발동 횟수를 한 번의 쿼리로 배치 조회한다(N+1 방지).
    @Transactional(readOnly = true)
    public Map<Long, Long> countTheftEventsByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        refreshTokenTheftEventRepository.countGroupedByUserIdIn(userIds)
                .forEach(row -> counts.put(row.getUserId(), row.getCount()));
        return counts;
    }

    private String sanitizeIp(String requestIp) {
        return StringUtils.hasText(requestIp) ? requestIp : UNKNOWN_IP;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
