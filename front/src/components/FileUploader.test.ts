import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import FileUploader from './FileUploader.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {post: vi.fn()},
}))

const mockPost = apiClient.post as ReturnType<typeof vi.fn>

beforeEach(() => vi.clearAllMocks())

describe('FileUploader', () => {
    it('파일 1개 업로드 시 uploaded 이벤트로 jobId를 emit한다', async () => {
        mockPost.mockResolvedValueOnce({data: {jobId: 'job-abc'}})

        const wrapper = mount(FileUploader, {
            props: {moduleId: 'image-to-pdf'},
        })

        const file = new File(['data'], 'test.jpg', {type: 'image/jpeg'})
        const input = wrapper.find('input[type="file"]').element as HTMLInputElement
        Object.defineProperty(input, 'files', {value: [file], configurable: true})
        await wrapper.find('input[type="file"]').trigger('change')
        await flushPromises()

        expect(wrapper.emitted('uploaded')).toBeTruthy()
        expect(wrapper.emitted('uploaded')![0]).toEqual([{jobId: 'job-abc'}])
    })
})
