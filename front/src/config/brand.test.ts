import {describe, expect, it} from 'vitest'
import {BRAND} from './brand'

describe('BRAND', () => {
    it('사이트명은 OnTool이다', () => {
        expect(BRAND.siteName).toBe('OnTool')
    })

    it('워드마크는 소문자 ontool이다', () => {
        expect(BRAND.wordmark).toBe('ontool')
    })

    it('한글 병기명은 온툴이다', () => {
        expect(BRAND.koreanName).toBe('온툴')
    })

    it('슬로건은 "모든 도구, 한 곳에"이다', () => {
        expect(BRAND.slogan).toBe('모든 도구, 한 곳에')
    })
})
