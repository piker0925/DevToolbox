package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiHashModuleTest {

    @Test
    void returnsAllFourHashes() {
        MultiHashModule module = new MultiHashModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("text", "hello")
        ));

        assertThat(result.isFile()).isFalse();
        // 각 알고리즘 라벨에 그 알고리즘의 올바른 해시값이 붙어야 한다.
        // (독립적으로 계산한 "hello"의 알려진 해시값 — 4개 슬롯에 같은 값이 들어가면 실패한다.)
        assertThat(result.textResult()).contains(
                "MD5: 5d41402abc4b2a76b9719d911017c592");
        assertThat(result.textResult()).contains(
                "SHA-1: aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d");
        assertThat(result.textResult()).contains(
                "SHA-256: 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(result.textResult()).contains(
                "SHA-512: 9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043");
    }

    @Test
    void moduleMetadata() {
        MultiHashModule module = new MultiHashModule();
        assertThat(module.getId()).isEqualTo("multi-hash");
        assertThat(module.isHeavy()).isFalse();
    }
}
