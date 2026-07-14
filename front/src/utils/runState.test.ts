import {describe, expect, it} from 'vitest'
import {clearPreviousRun, type RunState} from './runState'

// 완전히 비워진 상태 — 어떤 직전 상태에서 재업로드하든 이 형태로 수렴해야 한다.
const EMPTY: RunState = {
    jobId: null,
    jobProgress: null,
    batchId: null,
    batchProgress: null,
    batchComplete: false,
    result: null,
    runError: '',
}

describe('clearPreviousRun — 재업로드 시 직전 실행 잔여 상태 제거', () => {
    it('단일 완료 상태(단일→단일 재업로드)에서 result·jobId·jobProgress 잔여가 사라진다', () => {
        const prev: RunState = {
            jobId: 'job-1',
            jobProgress: {queuePosition: 0, progress: 100, etaSeconds: 0},
            batchId: null,
            batchProgress: null,
            batchComplete: false,
            result: {url: 'http://x/result.pdf', text: null},
            runError: '',
        }
        expect(clearPreviousRun(prev)).toEqual(EMPTY)
    })

    it('배치 완료 상태(배치→단일 재업로드)에서 batchId·batchProgress·batchComplete 잔여가 사라진다', () => {
        const prev: RunState = {
            jobId: null,
            jobProgress: null,
            batchId: 'batch-9',
            batchProgress: {batchId: 'batch-9', totalCount: 3, doneCount: 3, failCount: 0},
            batchComplete: true,
            result: null,
            runError: '',
        }
        const next = clearPreviousRun(prev)
        // 반대쪽 경로(배치)의 잔여가 남으면 새 단일 업로드 화면이 옛날 배치 결과에 머문다 — 버그의 핵심.
        expect(next.batchId).toBeNull()
        expect(next.batchComplete).toBe(false)
        expect(next).toEqual(EMPTY)
    })

    it('처리 중 상태(단일→배치 재업로드)에서 jobId·jobProgress·runError 잔여가 사라진다', () => {
        const prev: RunState = {
            jobId: 'job-7',
            jobProgress: {queuePosition: 2, progress: 40, etaSeconds: 12},
            batchId: null,
            batchProgress: null,
            batchComplete: false,
            result: null,
            runError: '이전 실패 메시지',
        }
        expect(clearPreviousRun(prev)).toEqual(EMPTY)
    })
})
