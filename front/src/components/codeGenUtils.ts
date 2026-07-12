// 통합 코드 생성기(UnifiedCodeGenPage) 전용 헬퍼 — 형식별 검증·색상 유틸

export type CodeFormatId = 'qr' | 'code128' | 'ean13'

/** EAN-13 체크 디지트 계산: 앞 12자리 기준 (홀수 위치 ×1, 짝수 위치 ×3) */
export function ean13CheckDigit(digits12: string): number {
    let sum = 0
    for (let i = 0; i < 12; i++) {
        const d = digits12.charCodeAt(i) - 48
        sum += i % 2 === 0 ? d : d * 3
    }
    return (10 - (sum % 10)) % 10
}

/**
 * 형식별 입력 즉시 검증.
 * 통과하면 null, 실패하면 사용자에게 보여줄 한국어 안내 메시지를 반환한다.
 * (백엔드 BarcodeModule 검증과 동일한 규칙 — 프론트에서 미리 걸러 왕복을 줄인다)
 */
export function validateCodeContent(format: CodeFormatId, content: string): string | null {
    if (!content) return null
    if (format === 'ean13') {
        if (!/^\d{12,13}$/.test(content)) {
            return 'EAN-13은 숫자 12~13자리여야 합니다. 12자리 입력 시 체크 디지트를 자동 계산합니다.'
        }
        if (content.length === 13) {
            const expected = ean13CheckDigit(content.slice(0, 12))
            if (expected !== content.charCodeAt(12) - 48) {
                return `체크 디지트가 올바르지 않습니다. 마지막 자리는 ${expected}이어야 합니다.`
            }
        }
        return null
    }
    if (format === 'code128') {
        for (const ch of content) {
            if (ch.charCodeAt(0) > 127) {
                return `Code 128은 ASCII 문자(영문·숫자·기호)만 지원합니다. (허용되지 않는 문자: ${ch})`
            }
        }
        return null
    }
    // QR은 모든 텍스트 허용
    return null
}

/** "#RRGGBB" 또는 "RRGGBB" 형식인지 검사 */
export function isValidHexColor(v: string): boolean {
    return /^#?[0-9a-fA-F]{6}$/.test(v.trim())
}

/** hex 문자열을 "#RRGGBB" 형태로 정규화 (유효성 검사는 하지 않음) */
export function normalizeHex(v: string): string {
    const t = v.trim()
    return t.startsWith('#') ? t : `#${t}`
}

/** WCAG 상대 휘도 기반 대비율 (1~21). 스캐너 가독성 경고 판단에 사용 */
export function contrastRatio(hexA: string, hexB: string): number {
    const luminance = (hex: string): number => {
        const n = parseInt(normalizeHex(hex).slice(1), 16)
        const chan = (c: number): number => {
            const s = c / 255
            return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
        }
        return 0.2126 * chan((n >> 16) & 255) + 0.7152 * chan((n >> 8) & 255) + 0.0722 * chan(n & 255)
    }
    const l1 = luminance(hexA)
    const l2 = luminance(hexB)
    const [hi, lo] = l1 >= l2 ? [l1, l2] : [l2, l1]
    return (hi + 0.05) / (lo + 0.05)
}
