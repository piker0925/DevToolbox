import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import JobPoller from './JobPoller.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn()},
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

beforeEach(() => vi.clearAllMocks())

describe('JobPoller', () => {
    it('상태가 DONE이면 done 이벤트를 emit한다', async () => {
        mockGet.mockResolvedValueOnce({data: {id: 'job-1', status: 'DONE'}})

        const wrapper = mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })
        await flushPromises()

        expect(wrapper.emitted('done')).toBeTruthy()
        expect(wrapper.emitted('done')![0]).toEqual([{id: 'job-1', status: 'DONE'}])
    })

    it('상태가 FAILED이면 failed 이벤트를 emit한다', async () => {
        mockGet.mockResolvedValueOnce({data: {id: 'job-1', status: 'FAILED'}})

        const wrapper = mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })
        await flushPromises()

        expect(wrapper.emitted('failed')).toBeTruthy()
    })
})
