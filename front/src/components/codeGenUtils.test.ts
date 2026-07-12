import {describe, expect, it} from 'vitest'
import {contrastRatio, ean13CheckDigit, isValidHexColor, normalizeHex, validateCodeContent} from './codeGenUtils'

describe('ean13CheckDigit', () => {
    it('공지된 기준값과 일치한다', () => {
        // 4006381333931 — 널리 알려진 유효 EAN-13 예시
        expect(ean13CheckDigit('400638133393')).toBe(1)
        // 8801234567893 — 수기 계산 기준값
        expect(ean13CheckDigit('880123456789')).toBe(3)
    })

    it('합이 10의 배수면 체크 디지트는 0이다', () => {
        expect(ean13CheckDigit('000000000000')).toBe(0)
    })
})

describe('validateCodeContent', () => {
    it('EAN-13: 유효한 13자리는 통과, 잘못된 체크 디지트는 기대값을 안내한다', () => {
        expect(validateCodeContent('ean13', '4006381333931')).toBeNull()
        expect(validateCodeContent('ean13', '4006381333930')).toContain('마지막 자리는 1')
    })

    it('EAN-13: 12자리는 체크 디지트 자동 계산 대상으로 통과한다', () => {
        expect(validateCodeContent('ean13', '400638133393')).toBeNull()
    })

    it('EAN-13: 자릿수/문자 위반은 규칙을 안내한다', () => {
        expect(validateCodeContent('ean13', '12345')).toContain('12~13자리')
        expect(validateCodeContent('ean13', 'abcdefghijkl')).toContain('12~13자리')
        expect(validateCodeContent('ean13', '12345678901234')).toContain('12~13자리')
    })

    it('Code 128: ASCII는 통과, 비ASCII는 문제 문자를 짚어준다', () => {
        expect(validateCodeContent('code128', 'DEV-2026/0001 #A')).toBeNull()
        expect(validateCodeContent('code128', 'abc한글')).toContain('한')
    })

    it('QR은 모든 텍스트를 허용한다', () => {
        expect(validateCodeContent('qr', '한글 URL https://예시.com 😀')).toBeNull()
    })

    it('빈 입력은 검증하지 않는다', () => {
        expect(validateCodeContent('ean13', '')).toBeNull()
    })
})

describe('hex 색상 유틸', () => {
    it('isValidHexColor는 6자리 hex만 허용한다', () => {
        expect(isValidHexColor('#112233')).toBe(true)
        expect(isValidHexColor('AABBCC')).toBe(true)
        expect(isValidHexColor('#FFF')).toBe(false)
        expect(isValidHexColor('red')).toBe(false)
        expect(isValidHexColor('#12345G')).toBe(false)
    })

    it('normalizeHex는 # 접두사를 보장한다', () => {
        expect(normalizeHex('112233')).toBe('#112233')
        expect(normalizeHex(' #112233 ')).toBe('#112233')
    })
})

describe('contrastRatio', () => {
    it('흑백은 21, 동일 색은 1이다', () => {
        expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5)
        expect(contrastRatio('#808080', '#808080')).toBeCloseTo(1, 5)
    })

    it('낮은 대비 조합(노랑/흰색)은 3 미만, 파랑/흰색은 3 이상이다', () => {
        expect(contrastRatio('#FFFF00', '#FFFFFF')).toBeLessThan(3)
        expect(contrastRatio('#0000FF', '#FFFFFF')).toBeGreaterThan(3)
    })

    it('인자 순서와 무관하게 같은 값을 반환한다', () => {
        expect(contrastRatio('#112233', '#EEDDCC')).toBeCloseTo(contrastRatio('#EEDDCC', '#112233'), 10)
    })
})
