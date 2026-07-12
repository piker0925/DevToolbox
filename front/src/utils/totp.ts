// RFC 6238 TOTP / RFC 4226 HOTP — 브라우저 로컬 구현.
// secret은 서버로 전송하지 않고 crypto.subtle(HMAC-SHA1)로 코드를 계산한다.

const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

/** 공백·하이픈 제거 + 대문자 정규화 (Google Authenticator 표기 호환) */
export function normalizeSecret(secret: string): string {
    return secret.replace(/[\s-]/g, '').toUpperCase()
}

export interface SecretValidation {
    valid: boolean
    /** valid=false일 때 사용자에게 보여줄 한국어 메시지 */
    message?: string
}

/** Base32 secret 유효성 검사 (정규화 후 기준) */
export function validateSecret(secret: string): SecretValidation {
    const normalized = normalizeSecret(secret)
    if (normalized.length === 0) {
        return {valid: false, message: '시크릿 키를 입력하세요'}
    }
    const stripped = normalized.replace(/=+$/, '')
    const invalid = [...new Set([...stripped].filter((c) => !BASE32_ALPHABET.includes(c)))]
    if (invalid.length > 0) {
        return {
            valid: false,
            message: `Base32에 없는 문자: ${invalid.join(' ')} (허용: A-Z, 2-7)`,
        }
    }
    if (stripped.length < 8) {
        return {valid: false, message: '시크릿이 너무 짧습니다 (8자 이상 필요)'}
    }
    return {valid: true}
}

/** Base32(RFC 4648) 디코드. 유효하지 않은 문자는 예외 */
export function base32Decode(secret: string): Uint8Array {
    const normalized = normalizeSecret(secret).replace(/=+$/, '')
    let bits = 0
    let value = 0
    const bytes: number[] = []
    for (const char of normalized) {
        const index = BASE32_ALPHABET.indexOf(char)
        if (index < 0) throw new Error(`유효하지 않은 Base32 문자: ${char}`)
        value = (value << 5) | index
        bits += 5
        if (bits >= 8) {
            bytes.push((value >>> (bits - 8)) & 0xff)
            bits -= 8
        }
    }
    return new Uint8Array(bytes)
}

/** RFC 4226 HOTP — HMAC-SHA1 + dynamic truncation */
export async function hotp(key: Uint8Array, counter: number, digits: number): Promise<string> {
    const message = new Uint8Array(8)
    // JS number로 64비트 카운터 big-endian 직렬화 (2^53 미만 안전)
    let c = counter
    for (let i = 7; i >= 0; i--) {
        message[i] = c & 0xff
        c = Math.floor(c / 256)
    }
    const cryptoKey = await crypto.subtle.importKey(
        'raw',
        key.buffer.slice(key.byteOffset, key.byteOffset + key.byteLength) as ArrayBuffer,
        {name: 'HMAC', hash: 'SHA-1'},
        false,
        ['sign'],
    )
    const signature = new Uint8Array(await crypto.subtle.sign('HMAC', cryptoKey, message.buffer as ArrayBuffer))
    const offset = signature[signature.length - 1]! & 0x0f
    const binary =
        ((signature[offset]! & 0x7f) << 24) |
        (signature[offset + 1]! << 16) |
        (signature[offset + 2]! << 8) |
        signature[offset + 3]!
    return String(binary % 10 ** digits).padStart(digits, '0')
}

/** 특정 시각(epoch ms)의 TOTP 코드 */
export async function totpAt(
    secret: string,
    epochMs: number,
    digits = 6,
    periodSeconds = 30,
): Promise<string> {
    const key = base32Decode(secret)
    if (key.length === 0) throw new Error('시크릿 키가 비어 있습니다')
    const counter = Math.floor(epochMs / 1000 / periodSeconds)
    return hotp(key, counter, digits)
}

/** 현재 주기에서 코드가 유효한 남은 초 (1..period) */
export function remainingSeconds(epochMs: number, periodSeconds: number): number {
    return periodSeconds - (Math.floor(epochMs / 1000) % periodSeconds)
}
