package com.back.job.controller;

import com.back.global.storage.FileStorage;
import com.back.job.dto.JobResultResponse;
import com.back.job.dto.JobStatusResponse;
import com.back.job.entity.Job;
import com.back.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final FileStorage fileStorage;

    @GetMapping("/{id}")
    public JobStatusResponse getStatus(@PathVariable String id) {
        Job job = jobService.get(id);
        return new JobStatusResponse(job.getId(), job.getStatus().name(), job.getExpiresAt());
    }

    @GetMapping("/{id}/result")
    public JobResultResponse getResult(@PathVariable String id) {
        Job job = jobService.get(id);
        if (job.getResultKey() != null) {
            return new JobResultResponse(fileStorage.getUrl(job.getResultKey()), null);
        }
        return new JobResultResponse(null, job.getResultText());
    }
}
