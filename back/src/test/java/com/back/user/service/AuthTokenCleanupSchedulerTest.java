package com.back.user.service;

import com.back.global.security.jwt.RevokedAccessTokenRepository;
import com.back.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthTokenCleanupSchedulerTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Test
    void cleanup_두_테이블_모두_만료_기준으로_삭제를_호출한다() {
        AuthTokenCleanupScheduler scheduler = new AuthTokenCleanupScheduler(refreshTokenRepository, revokedAccessTokenRepository);

        LocalDateTime before = LocalDateTime.now();
        scheduler.cleanup();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> refreshCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteAllByExpiresAtBefore(refreshCutoff.capture());
        assertThat(refreshCutoff.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);

        ArgumentCaptor<LocalDateTime> revokedCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(revokedAccessTokenRepository).deleteAllByExpiresAtBefore(revokedCutoff.capture());
        assertThat(revokedCutoff.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
