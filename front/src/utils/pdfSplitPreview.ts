/**
 * 백엔드 PdfSplitModule의 파일명 규칙을 그대로 미러링해 "실행하면 어떤 파일이
 * 나오는지"를 입력 즉시 보여준다. 문법 오류에 대한 상세 에러 메시지는 미러링하지
 * 않는다 — 유효성 검증의 단일 기준은 백엔드로 남기고, 여기서는 파싱 실패 시
 * 그냥 미리보기를 비운다(null).
 */

interface PageRange {
    start: number
    end: number
}

function parsePageRanges(spec: string, totalPages: number): PageRange[] | null {
    const ranges: PageRange[] = []
    for (const rawToken of spec.split(',')) {
        const token = rawToken.trim()
        if (!token) return null

        const dash = token.indexOf('-')
        let start: number
        let end: number
        if (dash >= 0) {
            if (token.indexOf('-', dash + 1) >= 0) return null
            const startPart = token.slice(0, dash).trim()
            const endPart = token.slice(dash + 1).trim()
            start = Number.parseInt(startPart, 10)
            if (!Number.isFinite(start) || String(start) !== startPart) return null
            if (endPart === '') {
                end = totalPages
            } else {
                end = Number.parseInt(endPart, 10)
                if (!Number.isFinite(end) || String(end) !== endPart) return null
            }
        } else {
            start = Number.parseInt(token, 10)
            if (!Number.isFinite(start) || String(start) !== token) return null
            end = start
        }

        if (start < 1 || start > totalPages || end < start) return null
        ranges.push({start, end: Math.min(end, totalPages)})
    }
    return ranges
}

const pad3 = (n: number) => String(n).padStart(3, '0')

/** 원본 파일명에서 확장자를 떼고 표시에 부적절한 문자만 가볍게 치환한다(미리보기용 — 실제 안전성 검증은 백엔드 담당). */
function baseNameFrom(fileName: string): string {
    const withoutExt = fileName.replace(/\.pdf$/i, '')
    const sanitized = withoutExt.replace(/[^\p{L}\p{N}._ -]/gu, '_').trim()
    return sanitized === '' ? 'split' : sanitized
}

function namesPerPage(ranges: PageRange[], baseName: string): string[] {
    const pages = new Set<number>()
    for (const r of ranges) {
        for (let p = r.start; p <= r.end; p++) pages.add(p)
    }
    return [...pages].map(p => `${baseName}-${pad3(p)}.pdf`)
}

function namesPerRange(ranges: PageRange[], baseName: string): string[] {
    const seen = new Set<string>()
    const names: string[] = []
    for (const r of ranges) {
        const name = r.start === r.end
            ? `${baseName}-${pad3(r.start)}.pdf`
            : `${baseName}-${pad3(r.start)}-${pad3(r.end)}.pdf`
        if (seen.has(name)) continue
        seen.add(name)
        names.push(name)
    }
    return names
}

/** pageRange/groupMode 입력으로 실제 생성될 ZIP 엔트리 파일명 목록을 미리 계산한다. 파싱 실패 시 null. */
export function previewSplitFileNames(
    pageRangeSpec: string, groupMode: string, totalPages: number, fileName: string,
): string[] | null {
    if (totalPages < 1) return null
    const ranges = pageRangeSpec.trim() === ''
        ? [{start: 1, end: totalPages}]
        : parsePageRanges(pageRangeSpec, totalPages)
    if (!ranges) return null
    const baseName = baseNameFrom(fileName)
    return groupMode === '구간' ? namesPerRange(ranges, baseName) : namesPerPage(ranges, baseName)
}
