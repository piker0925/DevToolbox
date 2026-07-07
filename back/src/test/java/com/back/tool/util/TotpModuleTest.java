package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TotpModuleTest {

    @Test
    void generatesSixDigitCode() {
        TotpModule module = new TotpModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("secret", "JBSWY3DPEHPK3PXP")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).matches(".*\\d{6}.*");
    }

    @Test
    void moduleMetadata() {
        TotpModule module = new TotpModule();
        assertThat(module.getId()).isEqualTo("totp");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("util");
    }
}
