package com.back.job.dto;

import java.time.LocalDateTime;

public record JobStatusResponse(String id, String status, LocalDateTime expiresAt) {
}
