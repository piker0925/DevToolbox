package com.back.comment.repository;

import com.back.comment.entity.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CommentRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    CommentRepository commentRepository;

    @BeforeEach
    void cleanup() {
        commentRepository.deleteAll();
    }

    @Test
    void findAllByOrderByCreatedAtDesc_returnsCommentsNewestFirst() {
        Comment older = new Comment();
        older.setModuleId("sha256");
        older.setContent("먼저 작성된 댓글");
        commentRepository.save(older);

        Comment newer = new Comment();
        newer.setModuleId("cron");
        newer.setContent("나중에 작성된 댓글");
        commentRepository.save(newer);

        List<Comment> result = commentRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("나중에 작성된 댓글");
        assertThat(result.get(1).getContent()).isEqualTo("먼저 작성된 댓글");
    }
}
