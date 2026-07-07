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
        assertThat(result.textResult()).contains("-world");
        assertThat(result.textResult()).contains("+Java");
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
