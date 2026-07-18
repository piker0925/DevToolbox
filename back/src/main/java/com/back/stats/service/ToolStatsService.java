package com.back.stats.service;

import com.back.job.entity.JobStatus;
import com.back.job.repository.JobRepository;
import com.back.stats.entity.ToolStats;
import com.back.stats.repository.ToolStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolStatsService {

    private final ToolStatsRepository toolStatsRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<ToolStats> findAll() {
        return toolStatsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long getFailCount(String moduleId) {
        return jobRepository.countByModuleIdAndStatus(moduleId, JobStatus.FAILED);
    }

    @Transactional
    public ToolStats getOrCreate(String moduleId) {
        return toolStatsRepository.findById(moduleId)
                .orElseGet(() -> {
                    toolStatsRepository.insertIfAbsent(moduleId);
                    return toolStatsRepository.findById(moduleId).orElseThrow();
                });
    }

    @Transactional
    public void incrementUseCount(String moduleId) {
        toolStatsRepository.upsertIncrementUseCount(moduleId);
    }

    @Transactional
    public void incrementLikeCount(String moduleId) {
        toolStatsRepository.upsertIncrementLikeCount(moduleId);
    }

    @Transactional
    public void decrementLikeCount(String moduleId) {
        toolStatsRepository.decrementLikeCount(moduleId);
    }
}
