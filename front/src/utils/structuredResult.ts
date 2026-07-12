// 백엔드 ToolResult.ofJson() 구조화 결과 컨벤션 파서.
// 표준 형태: {"type":"table",...} | {"type":"keyvalue",...} | 도구 특화 type.

export interface StructuredTable {
    type: 'table'
    columns: string[]
    rows: string[][]
}

export interface StructuredKeyValue {
    type: 'keyvalue'
    items: Array<{ key: string; value: string }>
}

export type StructuredResult =
    | StructuredTable
    | StructuredKeyValue
    | { type: string; [key: string]: unknown }

/** 결과 텍스트가 구조화 JSON이면 파싱해 반환, 아니면 null (일반 텍스트로 처리) */
export function parseStructuredResult(text: string | null | undefined): StructuredResult | null {
    if (!text) return null
    const trimmed = text.trim()
    if (!trimmed.startsWith('{')) return null
    try {
        const obj = JSON.parse(trimmed)
        return obj && typeof obj.type === 'string' ? obj : null
    } catch {
        return null
    }
}
