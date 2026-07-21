import {describe, expect, it} from 'vitest'
import {CATEGORY_CONFIG, CATEGORY_ORDER} from './categoryConfig'
import {MOCK_MODULES} from '../api/mock'

describe('categoryConfig — mock.ts와 정합성', () => {
    it('MOCK_MODULES에 쓰인 모든 category가 CATEGORY_ORDER에 존재한다', () => {
        const usedCategories = new Set(MOCK_MODULES.map(m => m.category))
        const missing = [...usedCategories].filter(c => !CATEGORY_ORDER.includes(c))

        expect(missing).toEqual([])
    })

    it('CATEGORY_ORDER의 모든 카테고리가 CATEGORY_CONFIG에도 정의돼 있다', () => {
        const missing = CATEGORY_ORDER.filter(name => !(name in CATEGORY_CONFIG))

        expect(missing).toEqual([])
    })
})
