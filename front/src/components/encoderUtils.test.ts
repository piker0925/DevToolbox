import {describe, expect, it} from 'vitest'
import {
    decodeCharCodes,
    decodeHex,
    decodeUnicode,
    encodeCharCodes,
    encodeHex,
    encodeUnicode,
    rot13,
} from './encoderUtils'

describe('encodeHex / decodeHex', () => {
    it('ASCII 텍스트를 정확한 hex로 인코드한다', () => {
        expect(encodeHex('hello')).toBe('68656c6c6f')
    })

    it('한글(UTF-8 멀티바이트)을 정확한 hex로 인코드한다', () => {
        expect(encodeHex('안')).toBe('ec9588')
    })

    it('hex를 원문으로 디코드한다 (인코드 결과가 원본과 다름을 함께 확인)', () => {
        const encoded = encodeHex('hello 안녕')
        expect(encoded).not.toBe('hello 안녕')
        expect(decodeHex(encoded)).toBe('hello 안녕')
    })

    it('공백이 섞인 hex도 디코드한다', () => {
        expect(decodeHex('68 65 6c 6c 6f')).toBe('hello')
    })

    it('유효하지 않은 hex 문자는 한국어 메시지로 실패한다', () => {
        expect(() => decodeHex('68zz')).toThrow('유효하지 않은 Hex 문자')
    })

    it('홀수 길이는 실패한다', () => {
        expect(() => decodeHex('686')).toThrow('짝수')
    })

    it('UTF-8이 아닌 바이트 시퀀스는 실패한다', () => {
        expect(() => decodeHex('ff')).toThrow('UTF-8')
    })
})

describe('encodeUnicode / decodeUnicode', () => {
    it('비-ASCII만 \\uXXXX로 이스케이프하고 ASCII는 유지한다', () => {
        expect(encodeUnicode('안녕 hello')).toBe('\\uc548\\ub155 hello')
    })

    it('서로게이트 쌍(이모지)은 두 개의 \\uXXXX가 된다', () => {
        expect(encodeUnicode('😀')).toBe('\\ud83d\\ude00')
    })

    it('이스케이프를 원문으로 되돌린다 (중간값이 원본과 다름을 함께 확인)', () => {
        const escaped = encodeUnicode('한글 ok')
        expect(escaped).not.toBe('한글 ok')
        expect(decodeUnicode(escaped)).toBe('한글 ok')
    })

    it('이스케이프가 아닌 텍스트는 그대로 유지한다', () => {
        expect(decodeUnicode('plain text')).toBe('plain text')
    })
})

describe('encodeCharCodes / decodeCharCodes', () => {
    it('각 문자를 10진 코드 포인트로 변환한다', () => {
        expect(encodeCharCodes('Hi!')).toBe('72 105 33')
    })

    it('이모지는 하나의 코드 포인트로 취급한다', () => {
        expect(encodeCharCodes('😀')).toBe('128512')
    })

    it('코드 포인트를 텍스트로 되돌린다', () => {
        expect(decodeCharCodes('72 105 33')).toBe('Hi!')
        expect(decodeCharCodes('72,105,33')).toBe('Hi!')
    })

    it('숫자가 아닌 토큰은 한국어 메시지로 실패한다', () => {
        expect(() => decodeCharCodes('72 abc')).toThrow('숫자가 아닌 값')
    })

    it('범위를 벗어난 코드 포인트는 실패한다', () => {
        expect(() => decodeCharCodes('9999999')).toThrow('범위')
    })
})

describe('rot13', () => {
    it('대소문자를 각각 정확히 치환한다', () => {
        expect(rot13('Hello, World!')).toBe('Uryyb, Jbeyq!')
    })

    it('알파벳이 아닌 문자(한글·숫자)는 그대로 둔다', () => {
        expect(rot13('abc 안녕 123')).toBe('nop 안녕 123')
    })

    it('두 번 적용하면 원문이 된다 (한 번 적용한 값은 원본과 다름)', () => {
        const once = rot13('DevToolbox')
        expect(once).toBe('QriGbbyobk')
        expect(rot13(once)).toBe('DevToolbox')
    })
})
