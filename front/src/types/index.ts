/**
 * 검색 별칭. 문자열이면 단순 검색어,
 * 객체면 딥링크 지원 별칭 — 해당 별칭으로 검색해 선택하면 query가 붙은 URL로 이동한다.
 * 예: encoder의 {keyword: 'base64', query: 'mode=base64-encode'}
 */
export type ModuleKeyword = string | { keyword: string; query?: string }

export interface Module {
    id: string
    name: string
    category: string
    isHeavy: boolean
    isFrontendOnly?: boolean
    description?: string
    /** 검색 별칭 (통합 도구가 흡수한 기존 도구명 등) */
    keywords?: ModuleKeyword[]
    /** 노출 구역. 소속이 아니라 탐색 컨텍스트 — 복수 허용. zones[0]이 기본 구역 (ADR-0023) */
    zones: import('../config/zones').ZoneId[]
    /** 분류 라벨 전용 — 렌더 결정권 없음 (ADR-0026) */
    kind?: 'tool' | 'game' | 'viewer' | 'studio'
    /** 지정 시 이 컴포넌트로 렌더, 생략 시 ToolPage */
    component?: () => Promise<import('vue').Component>
}

export interface Job {
    id: string
    status: string
    resultUrl?: string | null
    resultText?: string | null
}

// 업로드 응답: 단건(Job 1개) 또는 배치(파일당 Job N개 → ZIP)
export type UploadResult =
    | { jobId: string }
    | { batchId: string; jobIds: string[] }

export interface BatchProgress {
    batchId: string
    totalCount: number
    doneCount: number
    failCount: number
}

export function isBatchResult(r: UploadResult): r is { batchId: string; jobIds: string[] } {
    return 'batchId' in r
}
