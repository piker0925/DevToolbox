package com.back.job.dto;

import java.time.LocalDateTime;

public record JobCreateResponse(String jobId, LocalDateTime expiresAt) {
}
