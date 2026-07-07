package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegexModuleTest {

    @Test
    void findsAllMatches() {
        RegexModule module = new RegexModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("pattern", "\\d+", "text", "abc123def456ghi")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("123");
        assertThat(result.textResult()).contains("456");
    }

    @Test
    void noMatchReturnsEmptyResult() {
        RegexModule module = new RegexModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("pattern", "\\d+", "text", "no digits here")
        ));

        assertThat(result.textResult()).contains("매치 없음");
    }

    @Test
    void moduleMetadata() {
        RegexModule module = new RegexModule();
        assertThat(module.getId()).isEqualTo("regex-tester");
        assertThat(module.isHeavy()).isFalse();
    }
}
