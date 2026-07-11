package com.back.admin;

import com.back.comment.entity.Comment;

import java.time.LocalDateTime;

public record AdminCommentResponse(Long id, String moduleId, String content, LocalDateTime createdAt) {

    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(comment.getId(), comment.getModuleId(), comment.getContent(), comment.getCreatedAt());
    }
}
