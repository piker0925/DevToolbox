package com.back.admin;

import com.back.user.entity.AuthProvider;
import com.back.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        AuthProvider provider,
        String nickname,
        String email,
        LocalDateTime createdAt,
        long theftEventCount
) {
    public static AdminUserResponse from(User user, long theftEventCount) {
        return new AdminUserResponse(user.getId(), user.getProvider(), user.getNickname(), user.getEmail(),
                user.getCreatedAt(), theftEventCount);
    }
}
