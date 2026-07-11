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
        // 값을 라벨과 함께 검증 — host/path를 서로 뒤바꾸거나 쿼리 k/v 짝을 틀리는 뮤턴트를 잡는다.
        assertThat(result.textResult()).contains("스킴:     https");
        assertThat(result.textResult()).contains("호스트:   example.com");
        assertThat(result.textResult()).contains("포트:     8080");
        assertThat(result.textResult()).contains("경로:     /api/v1");
        assertThat(result.textResult()).contains("프래그먼트: section");
        assertThat(result.textResult()).contains("  key = value");
        assertThat(result.textResult()).contains("  foo = bar");
    }

    @Test
    void parsesSimpleUrl() {
        UrlParserModule module = new UrlParserModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("url", "http://localhost/path")
        ));

        // 최소 URL: 경로/포트기본값/프래그먼트없음 필드가 올바른 값으로 채워지는지 확인.
        assertThat(result.textResult()).contains("스킴:     http");
        assertThat(result.textResult()).contains("호스트:   localhost");
        assertThat(result.textResult()).contains("경로:     /path");
        assertThat(result.textResult()).contains("포트:     (기본)");
        assertThat(result.textResult()).contains("프래그먼트: (없음)");
    }

    @Test
    void moduleMetadata() {
        UrlParserModule module = new UrlParserModule();
        assertThat(module.getId()).isEqualTo("url-parser");
        assertThat(module.isHeavy()).isFalse();
    }
}
