import {afterEach, describe, expect, it, vi} from 'vitest'
import {apiClient} from './client'

describe('apiClient', () => {
    afterEach(() => {
        vi.unstubAllEnvs()
        vi.resetModules()
    })

    it('VITE_API_BASE_URL이 없으면 로컬 백엔드를 기본값으로 사용한다', () => {
        expect(apiClient.defaults.baseURL).toBe('http://localhost:8080')
    })

    it('VITE_API_BASE_URL이 설정되면 그 값을 baseURL로 사용한다', async () => {
        vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com')
        vi.resetModules()

        const {apiClient: freshClient} = await import('./client')

        expect(freshClient.defaults.baseURL).toBe('https://api.example.com')
    })
})
