package com.back.global.ratelimit;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IP당 요청 빈도 게이트(040)의 순수 정책. 실제 IP 추출은 ClientIpResolver가 맡고,
 * 여기서는 "윈도우 내 카운트 대 한도"의 판정만 검증한다.
 */
class RateLimiterTest {

    @Test
    void 한도_이내면_통과한다() {
        assertThatCode(() -> RateLimiter.checkLimit(5, 10)).doesNotThrowAnyException();
    }

    @Test
    void 카운트가_한도와_같으면_통과한다() {
        // 경계: 같음은 초과가 아니다.
        assertThatCode(() -> RateLimiter.checkLimit(10, 10)).doesNotThrowAnyException();
    }

    @Test
    void 카운트가_한도를_초과하면_RATE_LIMITED를_던진다() {
        assertThatThrownBy(() -> RateLimiter.checkLimit(11, 10))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void 같은_IP가_한도를_넘으면_RATE_LIMITED를_던진다() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T00:00:00Z"));
        RateLimiter limiter = new RateLimiter(3, 60, clock);

        limiter.assertNotLimited("1.2.3.4");
        limiter.assertNotLimited("1.2.3.4");
        limiter.assertNotLimited("1.2.3.4");

        assertThatThrownBy(() -> limiter.assertNotLimited("1.2.3.4"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void 다른_IP는_서로_영향을_주지_않는다() {
        // 격리 검증: 한 IP가 한도를 넘겨도 다른 IP는 그대로 통과해야 한다.
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T00:00:00Z"));
        RateLimiter limiter = new RateLimiter(2, 60, clock);

        limiter.assertNotLimited("1.1.1.1");
        limiter.assertNotLimited("1.1.1.1");
        assertThatThrownBy(() -> limiter.assertNotLimited("1.1.1.1")) // 1.1.1.1은 한도 초과
                .isInstanceOf(AppException.class);

        // 2.2.2.2는 처음 요청이므로 여전히 통과해야 한다 — 1.1.1.1의 카운트에 전혀 영향받지 않는다.
        assertThatCode(() -> limiter.assertNotLimited("2.2.2.2")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.assertNotLimited("2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    void 윈도우가_지나면_카운트가_리셋되어_다시_허용한다() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T00:00:00Z"));
        RateLimiter limiter = new RateLimiter(2, 60, clock);

        limiter.assertNotLimited("1.2.3.4");
        limiter.assertNotLimited("1.2.3.4");
        assertThatThrownBy(() -> limiter.assertNotLimited("1.2.3.4")).isInstanceOf(AppException.class);

        clock.advance(61); // 윈도우(60초) 경과

        assertThatCode(() -> limiter.assertNotLimited("1.2.3.4")).doesNotThrowAnyException();
    }

    /** 테스트에서 시간 경과를 시뮬레이션하기 위한 조정 가능 Clock. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
