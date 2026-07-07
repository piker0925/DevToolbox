package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HmacModuleTest {

    @Test
    void producesKnownHmacSha256() {
        HmacModule module = new HmacModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello", "key", "secret", "algorithm", "HmacSHA256")
        ));

        assertThat(result.isFile()).isFalse();
        // HMAC-SHA256("hello", "secret") known value
        assertThat(result.textResult()).isEqualToIgnoringCase(
                "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b");
    }

    @Test
    void moduleMetadata() {
        HmacModule module = new HmacModule();
        assertThat(module.getId()).isEqualTo("hmac");
        assertThat(module.isHeavy()).isFalse();
    }
}
