package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class HmacModule implements ToolModule {

    @Override
    public String getId() { return "hmac"; }

    @Override
    public String getName() { return "HMAC 서명"; }

    @Override
    public String getCategory() { return "util"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String text = input.params().getOrDefault("text", "");
        String key = input.params().getOrDefault("key", "");
        String algorithm = input.params().getOrDefault("algorithm", "HmacSHA256");
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] result = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return ToolResult.ofText(HexFormat.of().formatHex(result));
        } catch (Exception e) {
            throw new ToolProcessingException("HMAC 생성 실패: " + e.getMessage(), e);
        }
    }
}
