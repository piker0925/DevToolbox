// 통합 페이지(UnifiedConvertPage 등) 전용 헬퍼

export type DataFormat = 'json' | 'yaml' | 'toml' | 'xml' | 'csv'

export const DATA_FORMATS: DataFormat[] = ['json', 'yaml', 'toml', 'xml', 'csv']

/**
 * 입력 텍스트의 데이터 형식을 경량 휴리스틱으로 감지한다.
 * 확신할 수 없으면 null을 반환한다 (자동 설정은 제안 수준으로만 사용).
 */
export function detectFormat(text: string): DataFormat | null {
    const t = text.trim()
    if (!t) return null

    // XML: 태그로 시작
    if (t.startsWith('<')) return 'xml'

    // JSON: 객체/배열 리터럴로 시작하고 실제로 파싱 가능
    if (t.startsWith('{') || t.startsWith('[')) {
        try {
            JSON.parse(t)
            return 'json'
        } catch {
            // '['로 시작하지만 JSON이 아니면 TOML [section] 헤더일 수 있으므로 계속 검사
            if (t.startsWith('{')) return null
        }
    }

    const lines = t.split('\n').map(l => l.trim()).filter(l => l && !l.startsWith('#'))
    if (lines.length === 0) return null

    // TOML: [section] 헤더 또는 key = value 만으로 구성
    const tomlLine = (l: string) => /^\[[^\]]+\]$/.test(l) || /^[\w."'-]+\s*=\s*\S/.test(l)
    if (lines.some(l => l.includes('=')) && lines.every(tomlLine)) return 'toml'

    // YAML: key: value / key: / - item 패턴
    const yamlLine = (l: string) => /^(- )?[^:#,]+:(\s|$)/.test(l) || /^- \S/.test(l)
    if (lines.some(l => /:(\s|$)/.test(l)) && lines.every(yamlLine)) return 'yaml'

    // CSV: 2줄 이상이며 모든 행의 콤마 개수가 동일 (열 2개 이상)
    const rows = t.split('\n').filter(l => l.trim())
    if (rows.length >= 2) {
        const cols = rows[0].split(',').length
        if (cols >= 2 && rows.every(r => r.split(',').length === cols)) return 'csv'
    }

    return null
}
