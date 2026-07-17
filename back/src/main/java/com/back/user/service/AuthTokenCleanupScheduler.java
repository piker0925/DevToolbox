package com.back.user.service;

import com.back.global.security.jwt.RevokedAccessTokenRepository;
import com.back.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// TtlCleanupScheduler(Job)와 같은 정리 주기를 재사용한다 — refresh_token/revoked_access_token 둘 다
// expires_at을 이미 갖고 있는데 지금까지 정리 스케줄러가 없어 만료된 행이 영구히 쌓이고 있었다.
@Component
@RequiredArgsConstructor
public class AuthTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Scheduled(fixedDelayString = "${scheduling.ttl.delay:60000}")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteAllByExpiresAtBefore(now);
        revokedAccessTokenRepository.deleteAllByExpiresAtBefore(now);
    }
}
