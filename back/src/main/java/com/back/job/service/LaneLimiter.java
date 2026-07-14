package com.back.job.service;

import com.back.tool.model.Lane;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * 레인별 동시 실행 상한을 Semaphore permit으로 강제한다 — ADR-0019.
 * 2 OCPU를 MySQL·JVM·워커가 공유하므로 비디오는 1개, Heavy는 2개만 동시 실행.
 * permit 수는 application.yml로 노출해 재배포 없이 튜닝한다.
 */
@Component
public class LaneLimiter {

    private final Map<Lane, Semaphore> permits = new EnumMap<>(Lane.class);

    public LaneLimiter(
            @Value("${scheduling.worker.lane.heavy:2}") int heavy,
            @Value("${scheduling.worker.lane.video:1}") int video) {
        permits.put(Lane.HEAVY, new Semaphore(heavy));
        permits.put(Lane.VIDEO, new Semaphore(video));
    }

    /** 해당 레인 permit을 1개 확보 시도. 성공 시 반드시 {@link #release}로 반납해야 한다. */
    public boolean tryAcquire(Lane lane) {
        return semaphore(lane).tryAcquire();
    }

    public void release(Lane lane) {
        semaphore(lane).release();
    }

    /** 지금 이 레인에서 추가로 시작할 수 있는 작업 수(가용 permit). 폴링 배치 크기 계산에 쓴다. */
    public int available(Lane lane) {
        return semaphore(lane).availablePermits();
    }

    private Semaphore semaphore(Lane lane) {
        Semaphore s = permits.get(lane);
        if (s == null) {
            throw new IllegalArgumentException("Unknown lane: " + lane);
        }
        return s;
    }
}
