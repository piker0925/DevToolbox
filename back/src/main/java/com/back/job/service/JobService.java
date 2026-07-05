package com.back.job.service;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import com.back.job.entity.Job;
import com.back.job.entity.JobStatus;
import com.back.job.repository.BatchStats;
import com.back.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public Job create(String moduleId, List<String> inputPaths, Map<String, String> params) {
        return create(moduleId, null, inputPaths, params);
    }

    public Job create(String moduleId, String batchId, List<String> inputPaths, Map<String, String> params) {
        Job job = new Job();
        job.setModuleId(moduleId);
        job.setBatchId(batchId);
        job.setStatus(JobStatus.PENDING);
        job.setInputPaths(inputPaths);
        job.setParams(params);
        return jobRepository.save(job);
    }

    public Job get(String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
    }

    public List<Job> getBatchJobs(String batchId) {
        return jobRepository.findAllByBatchId(batchId);
    }

    public BatchStats getBatchStats(String batchId) {
        return jobRepository.getBatchStats(batchId);
    }
}
