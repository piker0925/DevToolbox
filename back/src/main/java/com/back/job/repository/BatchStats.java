package com.back.job.repository;

public interface BatchStats {
    Long getTotal();

    Long getDoneCount();

    Long getFailCount();
}
