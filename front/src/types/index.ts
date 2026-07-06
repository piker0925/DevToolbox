export interface Module {
    id: string
    name: string
    category: string
    isHeavy: boolean
    description?: string
}

export interface Job {
    id: string
    status: string
    resultUrl?: string | null
    resultText?: string | null
}

export interface UploadResult {
    jobId: string
}
