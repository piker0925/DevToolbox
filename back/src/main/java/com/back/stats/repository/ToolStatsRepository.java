package com.back.stats.repository;

import com.back.stats.entity.ToolStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolStatsRepository extends JpaRepository<ToolStats, String> {
}
