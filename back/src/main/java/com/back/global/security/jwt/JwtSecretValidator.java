package com.back.global.security.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// prod 프로파일에서 JWT_SECRET 환경변수를 깜빡 빠뜨리면 application.yaml의 로컬 전용 기본값으로
// 그대로 기동돼버린다 — 그 값은 이 저장소에 커밋돼 있어 누구나 알 수 있으므로, 그 상태로 뜨면
// 토큰 위조로 전체 계정이 뚫린다. 실수로 새는 걸 막기 위해 기동 자체를 거부한다.
@Component
@Profile("prod")
public class JwtSecretValidator {

    static final String DEFAULT_DEV_SECRET = "local-dev-only-jwt-secret-please-change-0123456789abcdef";

    private final String secret;

    public JwtSecretValidator(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    void validate() {
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않아 로컬 기본값 그대로 기동하려 합니다. 운영 환경변수를 설정하세요.");
        }
    }
}
