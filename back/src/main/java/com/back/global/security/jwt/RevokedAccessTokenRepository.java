package com.back.global.security.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    @Transactional
    void deleteAllByExpiresAtBefore(LocalDateTime dateTime);
}
