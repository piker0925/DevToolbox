package com.back.job.dto;

import java.time.LocalDateTime;

/**
 * 작업 상태 + 진행 가시화 (ADR-0019).
 * queuePosition: 같은 레인에서 내 앞의 대기 수(PENDING일 때만 의미). progress: 0~100.
 * etaSeconds: 진행률 있는 RUNNING에서만 채워지고, 그 외엔 null(큐 순번으로 안내).
 */
public record JobStatusResponse(
        String id,
        String status,
        int queuePosition,
        int progress,
        Long etaSeconds,
        LocalDateTime expiresAt) {
}
