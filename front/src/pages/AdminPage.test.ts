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
    it('로그인 후 전체 댓글 목록을 불러와 모듈 id와 함께 렌더링한다', async () => {
        mockAdminEndpoints()

        const wrapper = mount(AdminPage)
        await loginAsAdmin(wrapper)

        expect(mockGet).toHaveBeenCalledWith('/admin/comments', expect.anything())
        expect(wrapper.text()).toContain('좋은 도구네요')
        expect(wrapper.text()).toContain('sha256')
    })
})
