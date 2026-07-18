package com.back.admin;

import com.back.job.entity.Job;
import com.back.job.entity.JobStatus;
import com.back.tool.model.Lane;

import java.time.LocalDateTime;

public record AdminJobResponse(String id, String moduleId, Lane lane, JobStatus status, LocalDateTime createdAt) {

    public static AdminJobResponse from(Job job) {
        return new AdminJobResponse(job.getId(), job.getModuleId(), job.getLane(), job.getStatus(), job.getCreatedAt());
    }
}
