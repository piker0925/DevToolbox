// regex-tester 로컬 JS 엔진 — 순수 함수 모음 (JavaScript RegExp 기반)

export interface RegexGroup {
    /** 캡처 그룹 번호 (1부터) */
    num: number
    /** 명명 그룹 이름 ((?<name>...) 구문), 없으면 null */
    name: string | null
    /** 매치된 텍스트, 그룹이 참여하지 않았으면 null */
    text: string | null
}

export interface RegexMatchInfo {
    /** 매치 순번 (0부터) */
    index: number
    text: string
    start: number
    end: number
    groups: RegexGroup[]
}

export interface MatchOutcome {
    matches: RegexMatchInfo[]
    /** 컴파일 실패 시 SyntaxError 메시지, 정상이면 '' */
    error: string
    /** maxMatches 초과로 잘렸는지 */
    truncated: boolean
}

export interface ReplaceOutcome {
    result: string
    error: string
    /** 치환된 매치 개수 */
    count: number
}

export interface TextSegment {
    text: string
    /** 매치 순번, 매치 밖 구간이면 null */
    matchIndex: number | null
}

const DEFAULT_MAX_MATCHES = 1000

/**
 * 패턴 소스에서 캡처 그룹의 이름을 순서대로 추출한다.
 * 일반 그룹은 null, 명명 그룹은 이름. 비캡처/룩어라운드 그룹은 제외.
 */
export function captureGroupNames(pattern: string): (string | null)[] {
    const names: (string | null)[] = []
    let inClass = false
    for (let i = 0; i < pattern.length; i++) {
        const c = pattern[i]
        if (c === '\\') {
            i++ // 이스케이프된 다음 문자 건너뜀
            continue
        }
        if (inClass) {
            if (c === ']') inClass = false
            continue
        }
        if (c === '[') {
            inClass = true
            continue
        }
        if (c !== '(') continue
        if (pattern[i + 1] !== '?') {
            names.push(null) // 일반 캡처 그룹
            continue
        }
        // (?<name>...) — 룩비하인드 (?<= (?<! 는 캡처가 아님
        if (pattern[i + 2] === '<' && pattern[i + 3] !== '=' && pattern[i + 3] !== '!') {
            const close = pattern.indexOf('>', i + 3)
            names.push(close === -1 ? null : pattern.slice(i + 3, close))
        }
        // (?: (?= (?! 등은 비캡처 → 무시
    }
    return names
}

function compile(pattern: string, flags: string): { re: RegExp | null; error: string } {
    try {
        return {re: new RegExp(pattern, flags), error: ''}
    } catch (e) {
        return {re: null, error: (e as Error).message}
    }
}

/**
 * 텍스트에서 매치 목록을 찾는다.
 * g 플래그가 없으면 첫 매치만 반환한다 (JS RegExp 의미론 유지).
 */
export function findMatches(
    pattern: string,
    flags: string,
    text: string,
    maxMatches: number = DEFAULT_MAX_MATCHES,
): MatchOutcome {
    if (!pattern) return {matches: [], error: '', truncated: false}
    const {re, error} = compile(pattern, flags)
    if (!re) return {matches: [], error, truncated: false}

    const names = captureGroupNames(pattern)
    const global = flags.includes('g')
    // 순회를 위해 내부적으로 g를 보장하되, g가 없으면 첫 매치만 취한다
    const iter = global ? re : new RegExp(pattern, flags + 'g')

    const matches: RegexMatchInfo[] = []
    let truncated = false
    let m: RegExpExecArray | null
    iter.lastIndex = 0
    while ((m = iter.exec(text)) !== null) {
        if (matches.length >= maxMatches) {
            truncated = true
            break
        }
        const groups: RegexGroup[] = []
        for (let g = 1; g < m.length; g++) {
            groups.push({
                num: g,
                name: names[g - 1] ?? null,
                text: m[g] ?? null,
            })
        }
        matches.push({
            index: matches.length,
            text: m[0],
            start: m.index,
            end: m.index + m[0].length,
            groups,
        })
        if (!global) break // g 없음 → 첫 매치만
        if (m[0] === '') iter.lastIndex++ // 빈 매치 무한루프 방지
    }
    return {matches, error: '', truncated}
}

/**
 * 치환 미리보기. $1, $&, $<name> 등 JS replace 치환 패턴을 그대로 지원한다.
 * g 플래그가 없으면 첫 매치만 치환된다.
 */
export function replacePreview(
    pattern: string,
    flags: string,
    text: string,
    replacement: string,
): ReplaceOutcome {
    if (!pattern) return {result: text, error: '', count: 0}
    const {re, error} = compile(pattern, flags)
    if (!re) return {result: '', error, count: 0}
    const count = findMatches(pattern, flags, text).matches.length
    return {result: text.replace(re, replacement), error: '', count}
}

/**
 * 하이라이트 렌더링용으로 텍스트를 매치/비매치 구간으로 쪼갠다.
 * matches는 findMatches 결과(시작 오프셋 오름차순)여야 한다.
 */
export function segmentText(text: string, matches: RegexMatchInfo[]): TextSegment[] {
    const segments: TextSegment[] = []
    let cursor = 0
    for (const m of matches) {
        if (m.start > cursor) segments.push({text: text.slice(cursor, m.start), matchIndex: null})
        if (m.end > m.start) segments.push({text: text.slice(m.start, m.end), matchIndex: m.index})
        cursor = Math.max(cursor, m.end)
    }
    if (cursor < text.length) segments.push({text: text.slice(cursor), matchIndex: null})
    return segments
}

// ── 자주 쓰는 패턴 프리셋 ─────────────────────────────────────────────────────

export interface RegexPreset {
    id: string
    label: string
    pattern: string
    flags: string
    sample: string
}

export const REGEX_PRESETS: RegexPreset[] = [
    {
        id: 'email',
        label: '이메일',
        pattern: '[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+',
        flags: 'g',
        sample: '문의: alice@example.com 또는 bob.lee@test.co.kr 로 보내주세요.',
    },
    {
        id: 'url',
        label: 'URL',
        pattern: 'https?://[\\w.-]+(?::\\d+)?(?:/[^\\s]*)?',
        flags: 'g',
        sample: '문서는 https://example.com/docs?page=1 참고, 로컬은 http://localhost:8080/api 입니다.',
    },
    {
        id: 'ipv4',
        label: 'IP 주소',
        pattern: '\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b',
        flags: 'g',
        sample: '서버 192.168.0.10 에서 게이트웨이 10.0.0.1 로 라우팅합니다.',
    },
    {
        id: 'date',
        label: '날짜 (YYYY-MM-DD)',
        pattern: '(\\d{4})-(\\d{2})-(\\d{2})',
        flags: 'g',
        sample: '시작일 2026-07-12, 종료일 2026-12-31 입니다.',
    },
    {
        id: 'phone-kr',
        label: '한국 전화번호',
        pattern: '01[016789]-?\\d{3,4}-?\\d{4}',
        flags: 'g',
        sample: '연락처: 010-1234-5678 또는 01198765432 로 문자 주세요.',
    },
    {
        id: 'uuid',
        label: 'UUID',
        pattern: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
        flags: 'gi',
        sample: '요청 ID: 550e8400-E29B-41d4-a716-446655440000 (대소문자 혼용)',
    },
]
