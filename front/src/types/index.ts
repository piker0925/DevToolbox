export interface Module {
    id: string
    name: string
    category: string
    isHeavy: boolean
    isFrontendOnly?: boolean
    description?: string
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
