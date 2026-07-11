package com.back.comment.repository;

import com.back.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByModuleIdOrderByCreatedAtDesc(String moduleId);

    List<Comment> findAllByOrderByCreatedAtDesc();
}
