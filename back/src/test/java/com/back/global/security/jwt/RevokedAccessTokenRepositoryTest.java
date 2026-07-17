package com.back.global.security.jwt;

import com.back.AbstractMySQLIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RevokedAccessTokenRepositoryTest extends AbstractMySQLIntegrationTest {

    @Autowired
    RevokedAccessTokenRepository revokedAccessTokenRepository;

    @BeforeEach
    void cleanup() {
        revokedAccessTokenRepository.deleteAll();
    }

    @Test
    void save_persistsAndFindsById() {
        revokedAccessTokenRepository.save(new RevokedAccessToken("hash-abc", LocalDateTime.now().plusMinutes(30)));

        assertThat(revokedAccessTokenRepository.existsById("hash-abc")).isTrue();
        assertThat(revokedAccessTokenRepository.existsById("hash-other")).isFalse();
    }

    @Test
    void deleteAllByExpiresAtBefore_만료된_것만_지우고_아직_유효한_것은_남긴다() {
        revokedAccessTokenRepository.save(new RevokedAccessToken("hash-expired", LocalDateTime.now().minusMinutes(1)));
        revokedAccessTokenRepository.save(new RevokedAccessToken("hash-valid", LocalDateTime.now().plusMinutes(30)));

        revokedAccessTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());

        assertThat(revokedAccessTokenRepository.existsById("hash-expired")).isFalse();
        assertThat(revokedAccessTokenRepository.existsById("hash-valid")).isTrue();
    }
}
