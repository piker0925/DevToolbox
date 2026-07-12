import {beforeAll, describe, expect, it} from 'vitest'
import {base32Decode, hotp, normalizeSecret, remainingSeconds, totpAt, validateSecret} from './totp'

// jsdom에는 crypto.subtle이 없으므로 Node webcrypto로 채운다 (브라우저에서는 기본 제공).
// @types/node가 이 프로젝트의 tsconfig에 포함되어 있지 않아 'node:crypto' 타입을 모르므로
// 모듈 경로를 문자열 변수로 우회해 vue-tsc 타입 체크 에러를 피한다.
beforeAll(async () => {
    if (!globalThis.crypto?.subtle) {
        const nodeCryptoModule = 'node:crypto'
        const {webcrypto} = await import(/* @vite-ignore */ nodeCryptoModule)
        Object.defineProperty(globalThis, 'crypto', {value: webcrypto, configurable: true})
    }
})

// RFC 6238 Appendix B 테스트 벡터의 secret: ASCII "12345678901234567890"의 Base32
const RFC_SECRET = 'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ'

describe('base32Decode', () => {
    it('RFC secret을 ASCII "12345678901234567890" 바이트로 디코드한다', () => {
        const expected = new TextEncoder().encode('12345678901234567890')
        expect([...base32Decode(RFC_SECRET)]).toEqual([...expected])
    })

    it('JBSWY3DPEHPK3PXP를 알려진 바이트로 디코드한다', () => {
        // 독립 계산값 (Python base64.b32decode)
        expect([...base32Decode('JBSWY3DPEHPK3PXP')]).toEqual([72, 101, 108, 108, 111, 33, 222, 173, 190, 239])
    })

    it('소문자·공백·하이픈을 정규화해 디코드한다', () => {
        expect([...base32Decode('jbsw y3dp-ehpk 3pxp')]).toEqual([...base32Decode('JBSWY3DPEHPK3PXP')])
    })

    it('유효하지 않은 문자는 예외를 던진다', () => {
        expect(() => base32Decode('JBSW1')).toThrow(/Base32/)
    })
})

describe('totpAt — RFC 6238 Appendix B 테스트 벡터 (SHA-1, 8자리, 30초)', () => {
    const vectors: Array<[number, string]> = [
        [59, '94287082'],
        [1111111109, '07081804'],
        [1111111111, '14050471'],
        [1234567890, '89005924'],
        [2000000000, '69279037'],
    ]
    for (const [epochSec, expected] of vectors) {
        it(`T=${epochSec} → ${expected}`, async () => {
            expect(await totpAt(RFC_SECRET, epochSec * 1000, 8, 30)).toBe(expected)
        })
    }

    it('6자리 코드는 8자리 코드의 하위 6자리다 (독립 계산값)', async () => {
        // Python 독립 구현: JBSWY3DPEHPK3PXP, T=1111111111 → 358462
        expect(await totpAt('JBSWY3DPEHPK3PXP', 1111111111 * 1000, 6, 30)).toBe('358462')
    })

    it('이전/다음 30초 창은 서로 다른 코드를 만든다 (독립 계산값)', async () => {
        const t = 1111111111 * 1000
        expect(await totpAt('JBSWY3DPEHPK3PXP', t - 30_000, 6, 30)).toBe('071271')
        expect(await totpAt('JBSWY3DPEHPK3PXP', t + 30_000, 6, 30)).toBe('490635')
    })

    it('period=60이면 다른 카운터를 사용한다 (독립 계산값)', async () => {
        expect(await totpAt('JBSWY3DPEHPK3PXP', 1111111111 * 1000, 8, 60)).toBe('97912772')
    })
})

describe('hotp', () => {
    it('선행 0을 유지한다 (RFC 6238 T=1111111109 → 07081804)', async () => {
        const key = base32Decode(RFC_SECRET)
        expect(await hotp(key, Math.floor(1111111109 / 30), 8)).toBe('07081804')
    })
})

describe('validateSecret', () => {
    it('올바른 Base32는 통과한다', () => {
        expect(validateSecret('JBSWY3DPEHPK3PXP')).toEqual({valid: true})
        expect(validateSecret('jbsw y3dp ehpk 3pxp')).toEqual({valid: true})
    })

    it('빈 입력은 실패한다', () => {
        const r = validateSecret('   ')
        expect(r.valid).toBe(false)
        expect(r.message).toContain('입력')
    })

    it('Base32에 없는 문자(0, 1, 8)를 지적한다', () => {
        const r = validateSecret('JBSWY3DP018')
        expect(r.valid).toBe(false)
        expect(r.message).toContain('0')
        expect(r.message).toContain('1')
        expect(r.message).toContain('8')
    })

    it('너무 짧은 시크릿은 실패한다', () => {
        const r = validateSecret('JBSW')
        expect(r.valid).toBe(false)
        expect(r.message).toContain('짧')
    })
})

describe('normalizeSecret / remainingSeconds', () => {
    it('공백·하이픈 제거 + 대문자화', () => {
        expect(normalizeSecret(' jbsw-y3dp ')).toBe('JBSWY3DP')
    })

    it('남은 초: 주기 경계에서 period, 직전에는 1', () => {
        expect(remainingSeconds(0, 30)).toBe(30)
        expect(remainingSeconds(29_000, 30)).toBe(1)
        expect(remainingSeconds(30_000, 30)).toBe(30)
        expect(remainingSeconds(59_000, 60)).toBe(1)
    })
})
