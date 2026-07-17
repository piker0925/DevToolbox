import {describe, expect, it} from 'vitest'
import {normalizeApiModules} from './modules'
import type {Module} from '../types'

describe('normalizeApiModules', () => {
    it('백엔드 응답에 zones가 없으면 MOCK_MODULES 메타에서 채운다', () => {
        const backendShaped: Module[] = [
            // 백엔드는 zones를 모른다 — Module 타입상 필수이지만 실제 API 응답엔 없는 필드라
            // 백엔드 원시 응답 형태를 흉내내기 위해 캐스팅으로 필드를 생략한다.
            {id: 'bcrypt', name: 'Bcrypt 해시', category: 'security', isHeavy: false} as unknown as Module,
        ]

        const result = normalizeApiModules(backendShaped)

        const bcrypt = result.find(m => m.id === 'bcrypt')
        expect(bcrypt?.zones).toEqual(['dev'])
    })

    it('백엔드 응답이 이미 zones를 갖고 있으면 그 값을 유지한다', () => {
        const backendShaped: Module[] = [
            {id: 'bcrypt', name: 'Bcrypt 해시', category: 'security', isHeavy: false, zones: ['dev', 'files']},
        ]

        const result = normalizeApiModules(backendShaped)

        expect(result.find(m => m.id === 'bcrypt')?.zones).toEqual(['dev', 'files'])
    })

    it('frontendOnly 모듈은 MOCK_MODULES에 정의된 zones를 그대로 갖는다', () => {
        const result = normalizeApiModules([])

        const uuid = result.find(m => m.id === 'uuid')
        expect(uuid?.zones).toEqual(['dev'])
    })
})
