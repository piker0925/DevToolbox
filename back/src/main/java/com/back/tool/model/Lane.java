package com.back.tool.model;

/**
 * 작업 자원 등급. 레인마다 동시 실행 상한(Semaphore permit)이 다르다 — ADR-0019.
 * 2 OCPU를 MySQL·JVM·워커가 공유하므로 비용이 큰 작업일수록 좁은 레인에 둔다.
 */
public enum Lane {
    /** 이미지·PDF 등 짧은 Heavy 작업. 기본값. */
    HEAVY,
    /** 영상 인코딩 등 코어를 통째로 오래 점유하는 작업. 동시 1개만 허용. */
    VIDEO
}
