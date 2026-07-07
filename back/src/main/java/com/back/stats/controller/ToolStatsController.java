package com.back.stats.controller;

import com.back.stats.dto.ToolStatsResponse;
import com.back.stats.service.ToolStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tools/{moduleId}")
@RequiredArgsConstructor
public class ToolStatsController {

    private final ToolStatsService toolStatsService;

    @GetMapping("/stats")
    public ToolStatsResponse getStats(@PathVariable String moduleId) {
        return ToolStatsResponse.from(toolStatsService.getOrCreate(moduleId));
    }

    @PostMapping("/like")
    public ToolStatsResponse like(@PathVariable String moduleId) {
        toolStatsService.incrementLikeCount(moduleId);
        return ToolStatsResponse.from(toolStatsService.getOrCreate(moduleId));
    }
}
