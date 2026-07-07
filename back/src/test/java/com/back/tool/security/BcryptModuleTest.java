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
        assertThat(hash).startsWith("$2a$");
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
