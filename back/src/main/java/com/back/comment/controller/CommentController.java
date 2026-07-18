package com.back.comment.controller;

import com.back.comment.dto.CommentCreateRequest;
import com.back.comment.dto.CommentResponse;
import com.back.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools/{moduleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable String moduleId) {
        return commentService.getCommentResponses(moduleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable String moduleId,
                                     @RequestBody CommentCreateRequest request,
                                     @AuthenticationPrincipal Long userId) {
        return commentService.addCommentAndRespond(moduleId, request.content(), userId);
    }
}
