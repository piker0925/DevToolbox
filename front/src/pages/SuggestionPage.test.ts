import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import SuggestionPage from './SuggestionPage.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {post: vi.fn()},
}))

const mockPost = apiClient.post as ReturnType<typeof vi.fn>

beforeEach(() => vi.clearAllMocks())

describe('SuggestionPage', () => {
    it('건의사항을 입력하고 제출하면 /api/v1/suggestions로 전송된다', async () => {
        mockPost.mockResolvedValueOnce({data: {id: 1}})

        const wrapper = mount(SuggestionPage)
        await wrapper.find('textarea').setValue('다크모드 지원해주세요')
        await wrapper.find('button').trigger('click')
        await flushPromises()

        expect(mockPost).toHaveBeenCalledWith('/api/v1/suggestions', {content: '다크모드 지원해주세요'})
    })

    it('제출 성공 시 완료 메시지를 보여주고 입력창을 비운다', async () => {
        mockPost.mockResolvedValueOnce({data: {id: 1}})

        const wrapper = mount(SuggestionPage)
        await wrapper.find('textarea').setValue('다크모드 지원해주세요')
        await wrapper.find('button').trigger('click')
        await flushPromises()

        expect(wrapper.text()).toContain('감사합니다')

        await wrapper.find('button').trigger('click')
        expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('')
    })
})
