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
    void producesKnownHmacSha512() {
        HmacModule module = new HmacModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello", "key", "secret", "algorithm", "HmacSHA512")
        ));

        // algorithm 파라미터를 무시하고 항상 SHA256으로 고정하는 뮤턴트를 잡는다.
        // HMAC-SHA512("hello", "secret") known value (독립 계산)
        assertThat(result.textResult()).isEqualToIgnoringCase(
                "db1595ae88a62fd151ec1cba81b98c39df82daae7b4cb9820f446d5bf02f1dcfca6683d88cab3e273f5963ab8ec469a746b5b19086371239f67d1e5f99a79440");
    }

    @Test
    void moduleMetadata() {
        HmacModule module = new HmacModule();
        assertThat(module.getId()).isEqualTo("hmac");
        assertThat(module.isHeavy()).isFalse();
    }
}
