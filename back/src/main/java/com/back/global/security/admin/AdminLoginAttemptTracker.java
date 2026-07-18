package com.back.global.security.admin;

import com.back.global.ratelimit.FixedWindowCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 관리자 Basic Auth 브루트포스 방어 — IP별 실패 횟수를 고정 윈도우로 세어 한도 초과 시 잠근다.
 * "실패만 기록"(recordFailure)과 "요청마다 확인하되 기록하지 않음"(isLockedOut)이 분리돼 있어야
 * 한다 — 정상적으로 반복되는 요청(성공한 로그인, 관리자 화면 폴링)이 잠금 카운트를 소모하면 안
 * 되기 때문이다. 카운팅 메커니즘 자체는 {@link FixedWindowCounter}에 위임한다(RateLimiter와 공유).
 */
@Component
public class AdminLoginAttemptTracker {

    private final int maxFailures;
    private final FixedWindowCounter counter;

    @Autowired
    public AdminLoginAttemptTracker(
            @Value("${admin.login.max-failures:5}") int maxFailures,
            @Value("${admin.login.lockout-window-seconds:300}") long windowSeconds) {
        this(maxFailures, windowSeconds, Clock.systemUTC());
    }

    AdminLoginAttemptTracker(int maxFailures, long windowSeconds, Clock clock) {
        this.maxFailures = maxFailures;
        this.counter = new FixedWindowCounter(windowSeconds, clock);
    }

    /** 실패한 로그인 시도를 기록한다(AdminAuthenticationEntryPoint에서 BadCredentialsException일 때 호출). */
    public void recordFailure(String ip) {
        counter.increment(ip);
    }

    /** 기록 없이 현재 잠금 상태만 확인한다(요청 진입 시 AdminLoginLockoutFilter에서 호출). */
    public boolean isLockedOut(String ip) {
        return counter.peek(ip) >= maxFailures;
    }
}
