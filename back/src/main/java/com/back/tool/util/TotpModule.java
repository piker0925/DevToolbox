package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

@Component
public class TotpModule implements ToolModule {

    private static final TimeBasedOneTimePasswordGenerator TOTP = new TimeBasedOneTimePasswordGenerator();

    @Override
    public String getId() { return "totp"; }

    @Override
    public String getName() { return "TOTP 코드 생성"; }

    @Override
    public String getCategory() { return "util"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String secret = input.params().getOrDefault("secret", "");
        return ToolResult.ofText(generateAt(secret, Instant.now()));
    }

    /**
     * 지정한 시각의 TOTP 코드를 생성한다. 테스트에서 고정 Instant로 RFC 6238 벡터를 검증하기 위한 시임(seam).
     * process()는 이 메서드에 Instant.now()를 넘겨 위임한다 — 동작은 동일하다.
     */
    String generateAt(String secret, Instant when) {
        try {
            byte[] keyBytes = base32Decode(secret.toUpperCase().replaceAll("[^A-Z2-7]", ""));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
            int code = TOTP.generateOneTimePassword(keySpec, when);
            return String.format("%06d", code);
        } catch (Exception e) {
            throw new ToolProcessingException("TOTP 생성 실패: " + e.getMessage(), e);
        }
    }

    private byte[] base32Decode(String encoded) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int buffer = 0, bitsLeft = 0;
        byte[] result = new byte[encoded.length() * 5 / 8];
        int idx = 0;
        for (char c : encoded.toCharArray()) {
            int val = alphabet.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return java.util.Arrays.copyOf(result, idx);
    }
}
