package com.back.tool.formatter;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlFormatterModuleTest {

    @Test
    void formatsSelectStatement() {
        SqlFormatterModule module = new SqlFormatterModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("sql", "select id,name from users where id=1")
        ));

        assertThat(result.isFile()).isFalse();
        // JSqlParser 정규화: 키워드 대문자화, 콤마 뒤 공백, = 양쪽 공백.
        // 입력을 그대로 되돌리거나 절 순서를 망가뜨리는 뮤턴트는 이 exact-match에서 실패한다.
        assertThat(result.textResult())
                .isEqualTo("SELECT id, name FROM users WHERE id = 1");
    }

    @Test
    void invalidSqlThrows() {
        SqlFormatterModule module = new SqlFormatterModule();
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("sql", "this is not sql"))))
                .isInstanceOf(ToolProcessingException.class);
    }

    @Test
    void moduleMetadata() {
        SqlFormatterModule module = new SqlFormatterModule();
        assertThat(module.getId()).isEqualTo("sql-formatter");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("formatter");
    }
}
