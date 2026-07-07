package com.back.tool.network;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UrlParserModuleTest {

    @Test
    void parsesFullUrl() {
        UrlParserModule module = new UrlParserModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("url", "https://example.com:8080/api/v1?key=value&foo=bar#section")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("https");
        assertThat(result.textResult()).contains("example.com");
        assertThat(result.textResult()).contains("8080");
        assertThat(result.textResult()).contains("/api/v1");
        assertThat(result.textResult()).contains("key");
        assertThat(result.textResult()).contains("value");
    }

    @Test
    void parsesSimpleUrl() {
        UrlParserModule module = new UrlParserModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("url", "http://localhost/path")
        ));

        assertThat(result.textResult()).contains("http");
        assertThat(result.textResult()).contains("localhost");
    }

    @Test
    void moduleMetadata() {
        UrlParserModule module = new UrlParserModule();
        assertThat(module.getId()).isEqualTo("url-parser");
        assertThat(module.isHeavy()).isFalse();
    }
}
