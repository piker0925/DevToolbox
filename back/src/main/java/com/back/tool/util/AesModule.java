package com.back.tool.util;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesModule implements ToolModule {

    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    @Override
    public String getId() { return "aes"; }

    @Override
    public String getName() { return "AES 암호화/복호화"; }

    @Override
    public String getCategory() { return "util"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String text = input.params().getOrDefault("text", "");
        String key = input.params().getOrDefault("key", "");
        String mode = input.params().getOrDefault("mode", "encrypt");
        try {
            byte[] keyBytes = padKey(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            if ("decrypt".equals(mode)) {
                byte[] decoded = Base64.getDecoder().decode(text);
                byte[] iv = Arrays.copyOfRange(decoded, 0, 16);
                byte[] cipherText = Arrays.copyOfRange(decoded, 16, decoded.length);
                Cipher cipher = Cipher.getInstance(CIPHER);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
                return ToolResult.ofText(new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8));
            } else {
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                Cipher cipher = Cipher.getInstance(CIPHER);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
                byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
                byte[] combined = new byte[16 + encrypted.length];
                System.arraycopy(iv, 0, combined, 0, 16);
                System.arraycopy(encrypted, 0, combined, 16, encrypted.length);
                return ToolResult.ofText(Base64.getEncoder().encodeToString(combined));
            }
        } catch (Exception e) {
            throw new ToolProcessingException("AES 처리 실패: " + e.getMessage(), e);
        }
    }

    private byte[] padKey(byte[] key) {
        int len = key.length <= 16 ? 16 : key.length <= 24 ? 24 : 32;
        return Arrays.copyOf(key, len);
    }
}
