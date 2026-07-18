package com.back.admin;

import com.back.stats.entity.ToolStats;

public record AdminToolStatsResponse(String moduleId, long useCount, long likeCount, long failCount) {

    public static AdminToolStatsResponse from(ToolStats stats, long failCount) {
        return new AdminToolStatsResponse(stats.getModuleId(), stats.getUseCount(), stats.getLikeCount(), failCount);
    }
}
