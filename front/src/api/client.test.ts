import {describe, it, expect} from 'vitest'
import {apiClient} from './client'

describe('apiClient', () => {
    it('baseURL이 백엔드를 가리킨다', () => {
        expect(apiClient.defaults.baseURL).toBe('http://localhost:8080')
    })
})
