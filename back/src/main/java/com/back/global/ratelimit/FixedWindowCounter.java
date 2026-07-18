package com.back.global.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.base.Ticker;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * 키(IP 등)별 고정 윈도우 카운터 — Guava Cache로 비활성 키를 자동 만료시킨다.
 * "카운트를 세는" 메커니즘만 담당한다. 그 카운트로 무엇을 할지(매 호출마다 판정할지 —
 * {@link com.back.global.ratelimit.RateLimiter}, 기록과 확인을 분리할지 — 관리자 로그인
 * 잠금)는 호출자가 결정한다. RateLimiter와 관리자 로그인 브루트포스 방어가 거의 동일한
 * 카운팅 로직을 각자 구현하고 있던 것을 공통화했다.
 */
public class FixedWindowCounter {

    private final long windowMillis;
    private final Clock clock;
    private final Cache<String, WindowCounter> counters;

    public FixedWindowCounter(long windowSeconds, Clock clock) {
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
        // 윈도우의 2배 동안 접근이 없으면 만료 — 오래된 키가 맵에 무한정 쌓이는 것을 막는다.
        this.counters = CacheBuilder.newBuilder()
                .ticker(tickerFrom(clock))
                .expireAfterAccess(windowSeconds * 2, TimeUnit.SECONDS)
                .build();
    }

    /** key의 이번 윈도우 카운트를 1 증가시키고 증가 후 카운트를 반환한다. */
    public int increment(String key) {
        WindowCounter counter = counters.asMap()
                .compute(key, (_, existing) -> nextWindow(existing, clock.millis(), windowMillis));
        return counter.count();
    }

    /** 증가 없이 현재 윈도우의 카운트만 읽는다. 기록이 없거나 윈도우가 지났으면 0. */
    public int peek(String key) {
        WindowCounter counter = counters.getIfPresent(key);
        if (counter == null || clock.millis() - counter.windowStartMillis() >= windowMillis) {
            return 0;
        }
        return counter.count();
    }

    static WindowCounter nextWindow(WindowCounter existing, long nowMillis, long windowMillis) {
        if (existing == null || nowMillis - existing.windowStartMillis() >= windowMillis) {
            return new WindowCounter(nowMillis, 1);
        }
        return new WindowCounter(existing.windowStartMillis(), existing.count() + 1);
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
