package com.back.stats.dto;

import com.back.stats.entity.ToolStats;

public record ToolStatsResponse(String moduleId, long useCount, long likeCount) {

    public static ToolStatsResponse from(ToolStats stats) {
        return new ToolStatsResponse(stats.getModuleId(), stats.getUseCount(), stats.getLikeCount());
    }
}
