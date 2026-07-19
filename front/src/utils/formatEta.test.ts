import {describe, expect, it} from 'vitest'
import {formatEta} from './formatEta'

describe('formatEta', () => {
    it('60초 미만은 초 단위로 표시', () => {
        expect(formatEta(45)).toBe('45초')
    })

    it('0초 이하는 최소 1초로 올림 표시(0초 표시로 멈춘 것처럼 보이지 않게)', () => {
        expect(formatEta(0)).toBe('1초')
    })

    it('60초 이상은 분+초로 표시', () => {
        expect(formatEta(130)).toBe('2분 10초')
    })

    it('분 단위로 딱 떨어지면 초는 생략', () => {
        expect(formatEta(120)).toBe('2분')
    })
})
