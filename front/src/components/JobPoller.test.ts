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
    it('상태가 DONE이면 done 이벤트를 emit하고 폴링을 멈춘다', async () => {
        mockGet.mockResolvedValueOnce({data: {id: 'job-1', status: 'DONE'}})

        mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })
        await flushPromises()

        expect(mockGet).toHaveBeenCalledTimes(1)
        expect(mockGet).toHaveBeenCalledWith('/api/v1/jobs/job-1')

        // 폴링이 멈췄다면 시간이 흘러도 추가 호출이 없어야 한다.
        await new Promise(resolve => setTimeout(resolve, 20))
        expect(mockGet).toHaveBeenCalledTimes(1)
    })

    it('done 이벤트는 최종 Job 데이터를 담아 emit한다', async () => {
        mockGet.mockResolvedValueOnce({data: {id: 'job-1', status: 'DONE'}})

        const wrapper = mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })
        await flushPromises()

        expect(wrapper.emitted('done')).toBeTruthy()
        expect(wrapper.emitted('done')![0]).toEqual([{id: 'job-1', status: 'DONE'}])
    })

    it('상태가 FAILED이면 failed 이벤트를 emit하고 폴링을 멈춘다', async () => {
        mockGet.mockResolvedValueOnce({data: {id: 'job-1', status: 'FAILED'}})

        mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })
        await flushPromises()

        expect(mockGet).toHaveBeenCalledTimes(1)

        await new Promise(resolve => setTimeout(resolve, 20))
        expect(mockGet).toHaveBeenCalledTimes(1)
    })

    it('PENDING 상태에서는 계속 폴링하다가 DONE이 되면 멈춘다', async () => {
        mockGet
            .mockResolvedValueOnce({data: {id: 'job-1', status: 'PENDING'}})
            .mockResolvedValueOnce({data: {id: 'job-1', status: 'PENDING'}})
            .mockResolvedValueOnce({data: {id: 'job-1', status: 'DONE'}})

        const wrapper = mount(JobPoller, {
            props: {jobId: 'job-1', intervalMs: 0},
        })

        // setTimeout(fn, 0) 체인을 거쳐 세 번째 호출까지 도달할 때까지 기다린다.
        await vi.waitFor(() => expect(mockGet).toHaveBeenCalledTimes(3))
        await flushPromises()

        expect(wrapper.emitted('done')).toBeTruthy()
        expect(wrapper.emitted('failed')).toBeFalsy()

        await new Promise(resolve => setTimeout(resolve, 20))
        expect(mockGet).toHaveBeenCalledTimes(3)
    })
})
