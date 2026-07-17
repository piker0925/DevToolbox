package com.back.global.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    @Test
    void 로컬_기본_시크릿_그대로면_기동을_거부한다() {
        JwtSecretValidator validator = new JwtSecretValidator(JwtSecretValidator.DEFAULT_DEV_SECRET);

        assertThatThrownBy(validator::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 실제_시크릿으로_교체됐으면_기동을_막지_않는다() {
        JwtSecretValidator validator = new JwtSecretValidator("a-real-production-secret-value-0123456789");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
