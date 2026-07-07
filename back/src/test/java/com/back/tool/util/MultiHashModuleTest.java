package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiHashModuleTest {

    @Test
    void returnsAllFourHashes() {
        MultiHashModule module = new MultiHashModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).containsIgnoringCase("MD5");
        assertThat(result.textResult()).containsIgnoringCase("SHA-1");
        assertThat(result.textResult()).containsIgnoringCase("SHA-256");
        assertThat(result.textResult()).containsIgnoringCase("SHA-512");
        // MD5 of "hello"
        assertThat(result.textResult()).containsIgnoringCase("5d41402abc4b2a76b9719d911017c592");
    }

    @Test
    void moduleMetadata() {
        MultiHashModule module = new MultiHashModule();
        assertThat(module.getId()).isEqualTo("multi-hash");
        assertThat(module.isHeavy()).isFalse();
    }
}
