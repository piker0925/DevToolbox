package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AesModuleTest {

    @Test
    void encryptThenDecryptMatchesOriginal() {
        AesModule module = new AesModule();
        String plaintext = "hello world";
        String key = "mysecretkey12345";

        ToolResult encrypted = module.process(new ToolInput(
                List.of(), Map.of("text", plaintext, "key", key, "mode", "encrypt")
        ));
        assertThat(encrypted.isFile()).isFalse();
        assertThat(encrypted.textResult()).isNotEqualTo(plaintext);

        ToolResult decrypted = module.process(new ToolInput(
                List.of(), Map.of("text", encrypted.textResult(), "key", key, "mode", "decrypt")
        ));
        assertThat(decrypted.textResult()).isEqualTo(plaintext);
    }

    @Test
    void moduleMetadata() {
        AesModule module = new AesModule();
        assertThat(module.getId()).isEqualTo("aes");
        assertThat(module.isHeavy()).isFalse();
    }
}
