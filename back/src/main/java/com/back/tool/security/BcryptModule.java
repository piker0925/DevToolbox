package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptModule implements ToolModule {

    @Override
    public String getId() { return "bcrypt"; }

    @Override
    public String getName() { return "Bcrypt 해시"; }

    @Override
    public String getCategory() { return "security"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            String password = input.params().getOrDefault("password", "");
            int rounds = Integer.parseInt(input.params().getOrDefault("rounds", "10"));
            String hash = new BCryptPasswordEncoder(rounds).encode(password);
            return ToolResult.ofText(hash);
        } catch (Exception e) {
            throw new ToolProcessingException("Bcrypt 해시 실패: " + e.getMessage(), e);
        }
    }
}
