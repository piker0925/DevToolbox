package com.back.stats.repository;

import com.back.stats.entity.ToolStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ToolStatsRepository extends JpaRepository<ToolStats, String> {

    /** 동시 생성 경합을 피하는 존재 보장 (MySQL 전용) */
    @Modifying
    @Query(value = "insert ignore into tool_stats (module_id, use_count, like_count) values (:moduleId, 0, 0)",
            nativeQuery = true)
    void insertIfAbsent(@Param("moduleId") String moduleId);

    /*
     * 증가 연산은 단일 원자 UPSERT로 처리한다 (MySQL 전용).
     * read-modify-write는 lost update가 나고, INSERT IGNORE 후 별도 UPDATE는
     * 동시 요청에서 갭 락 데드락이 발생할 수 있다 — 한 문장이면 둘 다 없다.
     */

    @Modifying(clearAutomatically = true)
    @Query(value = "insert into tool_stats (module_id, use_count, like_count) values (:moduleId, 1, 0) " +
            "on duplicate key update use_count = use_count + 1", nativeQuery = true)
    void upsertIncrementUseCount(@Param("moduleId") String moduleId);

    @Modifying(clearAutomatically = true)
    @Query(value = "insert into tool_stats (module_id, use_count, like_count) values (:moduleId, 0, 1) " +
            "on duplicate key update like_count = like_count + 1", nativeQuery = true)
    void upsertIncrementLikeCount(@Param("moduleId") String moduleId);

    /** likeCount > 0 조건으로 0 밑으로 내려가지 않는다. 행이 없으면 아무 일도 하지 않는다. */
    @Modifying(clearAutomatically = true)
    @Query("update ToolStats s set s.likeCount = s.likeCount - 1 where s.moduleId = :moduleId and s.likeCount > 0")
    int decrementLikeCount(@Param("moduleId") String moduleId);
}
