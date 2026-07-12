package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MultiHashModule implements ToolModule {

    private static final String[] ALGORITHMS = {"MD5", "SHA-1", "SHA-256", "SHA-512"};

    @Override
    public String getId() { return "multi-hash"; }

    @Override
    public String getName() { return "다중 해시"; }

    @Override
    public String getCategory() { return "util"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String text = input.params().getOrDefault("text", "");
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Map<String, String> hashes = new LinkedHashMap<>();
        try {
            for (String algo : ALGORITHMS) {
                hashes.put(algo, HexFormat.of().formatHex(MessageDigest.getInstance(algo).digest(bytes)));
            }
        } catch (Exception e) {
            throw new ToolProcessingException("해시 생성 실패: " + e.getMessage(), e);
        }
        String result = hashes.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        return ToolResult.ofText(result);
    }
}
