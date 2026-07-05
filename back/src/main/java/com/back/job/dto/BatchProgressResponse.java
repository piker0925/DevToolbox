package com.back.job.dto;

public record BatchProgressResponse(String batchId, long totalCount, long doneCount, long failCount) {
}
