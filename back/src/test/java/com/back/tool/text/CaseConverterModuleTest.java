package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseConverterModuleTest {

    @Test
    void camelToSnake() {
        CaseConverterModule module = new CaseConverterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "helloWorldFoo", "from", "camel", "to", "snake")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).isEqualTo("hello_world_foo");
    }

    @Test
    void snakeToPascal() {
        CaseConverterModule module = new CaseConverterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello_world_foo", "from", "snake", "to", "pascal")
        ));

        assertThat(result.textResult()).isEqualTo("HelloWorldFoo");
    }

    @Test
    void camelToKebab() {
        CaseConverterModule module = new CaseConverterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "helloWorldFoo", "from", "camel", "to", "kebab")
        ));

        assertThat(result.textResult()).isEqualTo("hello-world-foo");
    }

    @Test
    void moduleMetadata() {
        CaseConverterModule module = new CaseConverterModule();
        assertThat(module.getId()).isEqualTo("case-converter");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("text");
    }
}
