// UnifiedEncoderPage 전용 추가 인코더/디코더 (Hex, Unicode escape, ASCII 코드, ROT13)

/** UTF-8 바이트를 소문자 hex 문자열로 인코드한다. */
export function encodeHex(input: string): string {
    return Array.from(new TextEncoder().encode(input))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
}

/** hex 문자열(공백 허용)을 UTF-8 텍스트로 디코드한다. */
export function decodeHex(input: string): string {
    const cleaned = input.replace(/\s+/g, '')
    if (cleaned.length === 0) return ''
    if (!/^[0-9a-fA-F]+$/.test(cleaned)) throw new Error('유효하지 않은 Hex 문자가 포함되어 있습니다')
    if (cleaned.length % 2 !== 0) throw new Error('Hex 문자열 길이는 짝수여야 합니다')
    const bytes = new Uint8Array(cleaned.length / 2)
    for (let i = 0; i < bytes.length; i++) {
        bytes[i] = parseInt(cleaned.slice(i * 2, i * 2 + 2), 16)
    }
    try {
        return new TextDecoder('utf-8', {fatal: true}).decode(bytes)
    } catch {
        throw new Error('UTF-8로 디코드할 수 없는 바이트 시퀀스입니다')
    }
}

/** 비-ASCII 문자를 \uXXXX 형태로 이스케이프한다 (서로게이트 쌍은 두 개의 \uXXXX). */
export function encodeUnicode(input: string): string {
    return input.replace(/[\u0080-\uffff]/g, c =>
        '\\u' + c.charCodeAt(0).toString(16).padStart(4, '0'),
    )
}

/** \uXXXX 이스케이프 시퀀스를 원래 문자로 되돌린다. */
export function decodeUnicode(input: string): string {
    return input.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex: string) =>
        String.fromCharCode(parseInt(hex, 16)),
    )
}

/** 각 문자를 10진 코드 포인트로 변환한다 (공백 구분). */
export function encodeCharCodes(input: string): string {
    return Array.from(input).map(c => c.codePointAt(0)).join(' ')
}

/** 공백/콤마로 구분된 10진 코드 포인트를 텍스트로 되돌린다. */
export function decodeCharCodes(input: string): string {
    const tokens = input.split(/[\s,]+/).filter(t => t.length > 0)
    if (tokens.length === 0) return ''
    const codes = tokens.map(t => {
        if (!/^\d+$/.test(t)) throw new Error(`숫자가 아닌 값이 있습니다: "${t}"`)
        const code = Number(t)
        if (code > 0x10ffff) throw new Error(`유효한 코드 포인트 범위(0~1114111)를 벗어났습니다: ${t}`)
        return code
    })
    return String.fromCodePoint(...codes)
}

/** ROT13 치환 (알파벳만, 자기 자신이 역함수). */
export function rot13(input: string): string {
    return input.replace(/[a-zA-Z]/g, c => {
        const base = c <= 'Z' ? 65 : 97
        return String.fromCharCode(((c.charCodeAt(0) - base + 13) % 26) + base)
    })
}
