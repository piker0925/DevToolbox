package com.back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class AsyncConfig {

    /**
     * 워커 실행 풀 — ADR-0019.
     * 동시 실행 상한은 실제로는 {@code LaneLimiter}의 레인별 permit(기본 VIDEO=1+HEAVY=2=3)이 결정한다.
     * 풀은 그 합을 담을 수 있게 잡되, 디스패치가 permit만큼만 제출하므로 큐가 찰 일이 없다.
     * CallerRunsPolicy는 제거한다 — poll()이 @Transactional이라 거부 작업을 poll 스레드의 열린
     * 트랜잭션 안에서 동기 처리하면 처리 내내 DB 락을 붙잡는 정합성 문제가 된다.
     * 대신 AbortPolicy로 두어(permit 설계상 거부는 발생하지 않아야 함) 문제가 조용히 묻히지 않게 한다.
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor(
            @Value("${scheduling.worker.lane.heavy:2}") int heavy,
            @Value("${scheduling.worker.lane.video:1}") int video) {
        int capacity = heavy + video;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(capacity);
        executor.setMaxPoolSize(capacity);
        executor.setQueueCapacity(capacity);
        executor.setThreadNamePrefix("worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
