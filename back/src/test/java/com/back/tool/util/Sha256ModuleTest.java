package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256ModuleTest {

    @Test
    void hashesKnownValue() {
        Sha256Module module = new Sha256Module();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello")
        ));

        assertThat(result.isFile()).isFalse();
        // SHA-256 of "hello" is known
        assertThat(result.textResult()).isEqualToIgnoringCase(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void moduleMetadata() {
        Sha256Module module = new Sha256Module();
        assertThat(module.getId()).isEqualTo("sha256");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("util");
    }
}
