package com.back.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tool_stats")
@Getter
@NoArgsConstructor
public class ToolStats {

    @Id
    @Column(length = 50)
    private String moduleId;

    @Setter
    @Column(nullable = false)
    private long useCount = 0;

    @Setter
    @Column(nullable = false)
    private long likeCount = 0;

    public ToolStats(String moduleId) {
        this.moduleId = moduleId;
    }
}
