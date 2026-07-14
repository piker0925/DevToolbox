package com.back.job.service;

import com.back.job.entity.Job;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공정 선택(selectFair) 순수 로직 검증 — ADR-0019.
 * 스프링 컨텍스트 없이 selectFair만 직접 호출한다(다른 의존성은 사용하지 않으므로 null 주입).
 */
class JobWorkerFairnessTest {

    private final JobWorker worker = new JobWorker(null, null, null, null, null, null);

    private Job ownedBy(String owner) {
        Job job = new Job();
        job.setOwnerToken(owner);
        return job;
    }

    @Test
    void selectFair_roundRobinsAcrossOwners_soLaterOwnerIsNotStarved() {
        // A가 창을 먼저·가득 채워도(a1,a2,a3), 뒤에 온 B(b1)가 굶으면 안 된다.
        // 두 행위자로 "공정 라운드로빈"과 "순수 FIFO"를 구분한다:
        //   - 공정: limit=2 → [a1, b1]  (B가 선택됨)
        //   - FIFO(잘못): limit=2 → [a1, a2] (B는 계속 밀림)
        Job a1 = ownedBy("A");
        Job a2 = ownedBy("A");
        Job a3 = ownedBy("A");
        Job b1 = ownedBy("B");
        List<Job> candidates = List.of(a1, a2, a3, b1); // created_at 오름차순 가정

        List<Job> chosen = worker.selectFair(candidates, 2);

        assertThat(chosen).hasSize(2);
        assertThat(chosen).containsExactly(a1, b1); // A의 최우선 + B(굶지 않음)
        assertThat(chosen).doesNotContain(a2);      // 같은 소유자가 연달아 독점하지 않음
    }

    @Test
    void selectFair_withinSameOwner_keepsFifoOrder() {
        // 한 소유자만 있으면 그 안에서는 오래된 순(FIFO)을 유지한다.
        Job a1 = ownedBy("A");
        Job a2 = ownedBy("A");
        Job a3 = ownedBy("A");

        List<Job> chosen = worker.selectFair(List.of(a1, a2, a3), 2);

        assertThat(chosen).containsExactly(a1, a2);
    }

    @Test
    void selectFair_limitLargerThanCandidates_returnsAll() {
        Job a1 = ownedBy("A");
        Job b1 = ownedBy("B");

        List<Job> chosen = worker.selectFair(List.of(a1, b1), 10);

        assertThat(chosen).containsExactlyInAnyOrder(a1, b1);
    }
}
