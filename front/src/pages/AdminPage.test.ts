import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import AdminPage from './AdminPage.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn(), delete: vi.fn()},
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

function mockAdminEndpoints() {
    mockGet.mockImplementation((url: string) => {
        if (url === '/admin/stats') return Promise.resolve({data: []})
        if (url === '/admin/suggestions') return Promise.resolve({data: []})
        if (url === '/admin/comments') {
            return Promise.resolve({
                data: [{id: 1, moduleId: 'sha256', content: '좋은 도구네요', createdAt: '2026-07-11T10:00:00'}],
            })
        }
        return Promise.reject(new Error('unexpected GET ' + url))
    })
}

async function loginAsAdmin(wrapper: ReturnType<typeof mount>) {
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('admin')
    await inputs[1].setValue('password')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
}

beforeEach(() => vi.clearAllMocks())

describe('AdminPage 댓글 관리', () => {
    it('운영 탭으로 전환하면 전체 댓글 목록을 불러와 모듈 id와 함께 렌더링한다', async () => {
        mockAdminEndpoints()

        const wrapper = mount(AdminPage)
        await loginAsAdmin(wrapper)

        // 댓글 관리는 "운영" 탭 안에 있다 — 관리자 화면이 3탭(통계/유저 관리/운영) 구조로
        // 리팩터링되면서 탭별 지연 로딩이 됐다(AI_SYNC.md 2026-07-18).
        const opsTab = wrapper.findAll('button').find(b => b.text().includes('운영'))
        await opsTab?.trigger('click')
        await flushPromises()

        expect(mockGet).toHaveBeenCalledWith('/admin/comments', expect.anything())
        expect(wrapper.text()).toContain('좋은 도구네요')
        expect(wrapper.text()).toContain('sha256')
    })
})
