package com.back.global.ratelimit;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.base.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * IP당 요청 빈도 앞단 거부(rate limiting, 040). Heavy 업로드 엔드포인트를 대상으로,
 * 고정 윈도우 카운터로 IP별 요청 수를 센다. AdmissionControl(036)과 같은 층위의 문 앞 거절이지만
 * 대상은 "시스템 용량"이 아니라 "클라이언트별 남용" — 판정(카운트 대 한도)은 정적 메서드로 순수하게 둔다.
 * 키(IP)별 상태를 들고 있어야 하므로 Guava Cache로 비활성 IP를 자동 만료시켜 무한정 누적을 막는다.
 */
@Component
public class RateLimiter {

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Cache<String, WindowCounter> counters;

    @Autowired
    public RateLimiter(
            @Value("${ratelimit.upload.max-per-window:200}") int limit,
            @Value("${ratelimit.upload.window-seconds:60}") long windowSeconds) {
        this(limit, windowSeconds, Clock.systemUTC());
    }

    RateLimiter(int limit, long windowSeconds, Clock clock) {
        this.limit = limit;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
        // 윈도우의 2배 동안 접근이 없으면 만료 — 오래된 IP가 맵에 무한정 쌓이는 것을 막는다.
        this.counters = CacheBuilder.newBuilder()
                .ticker(tickerFrom(clock))
                .expireAfterAccess(windowSeconds * 2, TimeUnit.SECONDS)
                .build();
    }

    /** clientKey(IP)의 이번 윈도우 카운트가 한도를 넘으면 RATE_LIMITED(429). */
    public void assertNotLimited(String clientKey) {
        WindowCounter counter = counters.asMap()
                .compute(clientKey, (key, existing) -> nextWindow(existing, clock.millis(), windowMillis));
        checkLimit(counter.count(), limit);
    }

    static WindowCounter nextWindow(WindowCounter existing, long nowMillis, long windowMillis) {
        if (existing == null || nowMillis - existing.windowStartMillis() >= windowMillis) {
            return new WindowCounter(nowMillis, 1);
        }
        return new WindowCounter(existing.windowStartMillis(), existing.count() + 1);
    }

    /** 카운트가 한도를 넘으면 RATE_LIMITED. 같음은 초과 아님. */
    static void checkLimit(int count, int limit) {
        if (count > limit) {
            throw new AppException(ErrorCode.RATE_LIMITED);
        }
    }

    private static Ticker tickerFrom(Clock clock) {
        return new Ticker() {
            @Override
            public long read() {
                return clock.millis() * 1_000_000L;
            }
        };
    }

    record WindowCounter(long windowStartMillis, int count) {}
}
