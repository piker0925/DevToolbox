package com.back.global.ratelimit;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * IP당 요청 빈도 앞단 거부(rate limiting, 040). Heavy 업로드 엔드포인트를 대상으로,
 * 고정 윈도우 카운터로 IP별 요청 수를 센다. AdmissionControl(036)과 같은 층위의 문 앞 거절이지만
 * 대상은 "시스템 용량"이 아니라 "클라이언트별 남용" — 판정(카운트 대 한도)은 정적 메서드로 순수하게 둔다.
 * 카운팅 메커니즘 자체는 {@link FixedWindowCounter}에 위임한다.
 */
@Component
public class RateLimiter {

    private final int limit;
    private final FixedWindowCounter counter;

    @Autowired
    public RateLimiter(
            @Value("${ratelimit.upload.max-per-window:200}") int limit,
            @Value("${ratelimit.upload.window-seconds:60}") long windowSeconds) {
        this(limit, windowSeconds, Clock.systemUTC());
    }

    RateLimiter(int limit, long windowSeconds, Clock clock) {
        this.limit = limit;
        this.counter = new FixedWindowCounter(windowSeconds, clock);
    }

    /** clientKey(IP)의 이번 윈도우 카운트가 한도를 넘으면 RATE_LIMITED(429). */
    public void assertNotLimited(String clientKey) {
        checkLimit(counter.increment(clientKey), limit);
    }

    /** 카운트가 한도를 넘으면 RATE_LIMITED. 같음은 초과 아님. */
    static void checkLimit(int count, int limit) {
        if (count > limit) {
            throw new AppException(ErrorCode.RATE_LIMITED);
        }
    }
}
