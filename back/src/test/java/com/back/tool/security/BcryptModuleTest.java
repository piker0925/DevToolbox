package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptModuleTest {

    @Test
    void hashMatchesOriginalPassword() {
        BcryptModule module = new BcryptModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("password", "secret123", "rounds", "4")
        ));

        assertThat(result.isFile()).isFalse();
        String hash = result.textResult();
        // 비용 factor가 요청한 rounds(4)를 반영해야 한다 — 기본값(10) 고정이면 이 assert가 실패한다.
        assertThat(hash).startsWith("$2a$04$");
        assertThat(new BCryptPasswordEncoder().matches("secret123", hash)).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("wrong", hash)).isFalse();
    }

    @Test
    void roundsParamControlsCostFactor() {
        BcryptModule module = new BcryptModule();
        String hash = module.process(new ToolInput(
                List.of(), Map.of("password", "secret123", "rounds", "6")
        )).textResult();

        // rounds가 다르면 비용 factor도 달라져야 한다 — 파라미터 무시 뮤턴트를 잡는다.
        assertThat(hash).startsWith("$2a$06$");
        assertThat(new BCryptPasswordEncoder().matches("secret123", hash)).isTrue();
    }

    @Test
    void moduleMetadata() {
        BcryptModule module = new BcryptModule();
        assertThat(module.getId()).isEqualTo("bcrypt");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("security");
    }
}
