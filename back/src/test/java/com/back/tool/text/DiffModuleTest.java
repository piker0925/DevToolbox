package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiffModuleTest {

    @Test
    void returnsUnifiedDiff() {
        DiffModule module = new DiffModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of(
                        "original", "hello\nworld",
                        "revised", "hello\nJava"
                )
        ));

        assertThat(result.isFile()).isFalse();
        // 전체 unified diff를 검증 — 헝크 헤더/컨텍스트 라인을 망가뜨리거나 순서를 바꾸는
        // 뮤턴트는 -world/+Java 부분 문자열만으로는 안 잡혀서 exact-match로 확인한다.
        assertThat(result.textResult()).isEqualTo(String.join("\n",
                "--- original",
                "+++ revised",
                "@@ -1,2 +1,2 @@",
                " hello",
                "-world",
                "+Java"));
    }

    @Test
    void identicalTextProducesEmptyDiff() {
        DiffModule module = new DiffModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("original", "same", "revised", "same")
        ));

        assertThat(result.textResult().trim()).isEmpty();
    }

    @Test
    void moduleMetadata() {
        DiffModule module = new DiffModule();
        assertThat(module.getId()).isEqualTo("text-diff");
        assertThat(module.isHeavy()).isFalse();
    }
}
