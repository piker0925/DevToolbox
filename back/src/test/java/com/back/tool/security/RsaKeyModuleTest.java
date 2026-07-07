package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyModuleTest {

    @Test
    void generatesRsa2048KeyPairInPem() {
        RsaKeyModule module = new RsaKeyModule();
        ToolResult result = module.process(new ToolInput(List.of(), Map.of("keyType", "RSA", "keySize", "2048")));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("-----BEGIN PUBLIC KEY-----");
        assertThat(result.textResult()).contains("-----BEGIN PRIVATE KEY-----");
    }

    @Test
    void generatesEcKeyPair() {
        RsaKeyModule module = new RsaKeyModule();
        ToolResult result = module.process(new ToolInput(List.of(), Map.of("keyType", "EC", "keySize", "256")));

        assertThat(result.textResult()).contains("-----BEGIN PUBLIC KEY-----");
        assertThat(result.textResult()).contains("-----BEGIN PRIVATE KEY-----");
    }

    @Test
    void moduleMetadata() {
        RsaKeyModule module = new RsaKeyModule();
        assertThat(module.getId()).isEqualTo("rsa-key");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("security");
    }
}
