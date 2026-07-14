import type {BatchProgress} from '../types'

/** 단건 작업 진행 가시화(ADR-0019): 큐 순번·진행률·ETA */
export interface JobProgress {
    queuePosition: number
    progress: number
    etaSeconds: number | null
}

/** Heavy 결과: 파일(url) 또는 텍스트 */
export interface RunResult {
    url: string | null
    text: string | null
}

/** 한 번의 실행에 관한 화면 상태 (단건·배치 공통). onUploaded 재진입 시 이 전체를 비운다. */
export interface RunState {
    jobId: string | null
    jobProgress: JobProgress | null
    batchId: string | null
    batchProgress: BatchProgress | null
    batchComplete: boolean
    result: RunResult | null
    runError: string
}

/**
 * 재업로드 시작 시 직전 실행의 잔여 상태를 전부 제거한 새 상태를 돌려준다.
 *
 * onUploaded가 새 jobId/batchId만 세팅하고 반대쪽 경로의 잔여(result·batchId·batchComplete 등)를
 * 남겨두면, 템플릿이 옛날 결과/배치 화면에 머무는 버그가 생긴다(033 문제 1). 직전 상태의 형태와
 * 무관하게 항상 완전히 빈 상태로 수렴시켜, 어느 경로(단일↔배치)로 재업로드하든 잔여가 없게 한다.
 */
export function clearPreviousRun(_prev: RunState): RunState {
    return {
        jobId: null,
        jobProgress: null,
        batchId: null,
        batchProgress: null,
        batchComplete: false,
        result: null,
        runError: '',
    }
}
