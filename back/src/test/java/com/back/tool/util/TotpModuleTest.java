package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TotpModuleTest {

    // RFC 6238 표준 테스트 secret: ASCII "12345678901234567890"의 Base32 인코딩.
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void generatesSixDigitCode() {
        TotpModule module = new TotpModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("secret", "JBSWY3DPEHPK3PXP")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).matches("\\d{6}");
    }

    @Test
    void matchesRfc6238TestVectors() {
        TotpModule module = new TotpModule();
        // RFC 6238 Appendix B: T=59s의 SHA-1 8자리 코드는 94287082 → 6자리는 287082.
        // 시간 스텝(30s)·다이제스트(SHA1)·자릿수 truncation이 틀리면 이 값이 안 나온다.
        assertThat(module.generateAt(RFC_SECRET, Instant.ofEpochSecond(59))).isEqualTo("287082");
        // T=1111111109s → RFC 8자리 07081804 → 6자리 081804.
        assertThat(module.generateAt(RFC_SECRET, Instant.ofEpochSecond(1111111109L))).isEqualTo("081804");
    }

    @Test
    void moduleMetadata() {
        TotpModule module = new TotpModule();
        assertThat(module.getId()).isEqualTo("totp");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("util");
    }
}
