package com.back.tool.formatter;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XmlFormatterModuleTest {

    private static final String COMPACT = "<root><child id=\"1\"><name>test</name></child></root>";

    @Test
    void formatsXmlWithIndent() {
        XmlFormatterModule module = new XmlFormatterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("xml", COMPACT, "minify", "false")
        ));

        assertThat(result.isFile()).isFalse();
        // 정확한 2-space 들여쓰기 구조. minify 분기가 잘못 실행되면(단일 라인 출력) 실패한다.
        assertThat(result.textResult()).isEqualTo(
                "<root>\n  <child id=\"1\">\n    <name>test</name>\n  </child>\n</root>");
    }

    @Test
    void minifiesXml() {
        String formatted = "<root>\n  <child id=\"1\">\n    <name>test</name>\n  </child>\n</root>";
        XmlFormatterModule module = new XmlFormatterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("xml", formatted, "minify", "true")
        ));

        assertThat(result.isFile()).isFalse();
        // 완전 압축: 줄바꿈이 하나도 없어야 한다. indent 분기가 잘못 실행되면 실패한다.
        assertThat(result.textResult()).isEqualTo(
                "<root><child id=\"1\"><name>test</name></child></root>");
    }

    @Test
    void moduleMetadata() {
        XmlFormatterModule module = new XmlFormatterModule();
        assertThat(module.getId()).isEqualTo("xml-formatter");
        assertThat(module.isHeavy()).isFalse();
    }
}
