package com.back.job.repository;

import com.back.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, String> {

    @Query(value = "SELECT * FROM job WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<Job> findFirstPendingWithLock();

    List<Job> findAllByExpiresAtBefore(LocalDateTime now);

    List<Job> findAllByBatchId(String batchId);

    @Query(value = "SELECT COUNT(*) as total, " +
            "COALESCE(SUM(status = 'DONE'), 0) as done_count, " +
            "COALESCE(SUM(status = 'FAILED'), 0) as fail_count " +
            "FROM job WHERE batch_id = :batchId",
            nativeQuery = true)
    BatchStats getBatchStats(@Param("batchId") String batchId);
}
