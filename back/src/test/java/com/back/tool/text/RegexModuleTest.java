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
        // 매치 개수와 각 매치의 [start-end] 오프셋까지 정확히 검증.
        // 모듈이 직접 계산하는 값이라 off-by-one(start/end)이나 개수 오류를 잡아야 한다.
        assertThat(result.textResult()).isEqualTo("2개 매치:\n[3-6] 123\n[9-12] 456");
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
