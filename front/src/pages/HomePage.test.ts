import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import HomePage from './HomePage.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn()},
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

const router = createRouter({
    history: createMemoryHistory(),
    routes: [{path: '/', component: HomePage}],
})

beforeEach(() => vi.clearAllMocks())

describe('HomePage', () => {
    it('모듈 목록을 가져와 카드로 렌더링한다', async () => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true},
                {id: 'sql-format', name: 'SQL 포맷터', category: '포맷터', isHeavy: false},
            ],
        })

        const wrapper = mount(HomePage, {global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('PDF 병합')
        expect(wrapper.text()).toContain('SQL 포맷터')
    })
})
