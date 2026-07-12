import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import BatchPoller from './BatchPoller.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn()},
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

beforeEach(() => vi.clearAllMocks())

function progress(total: number, done: number, fail: number) {
    return {data: {batchId: 'b-1', totalCount: total, doneCount: done, failCount: fail}}
}

describe('BatchPoller', () => {
    it('done+fail가 total과 같아지면 done 이벤트를 emit하고 폴링을 멈춘다', async () => {
        mockGet.mockResolvedValueOnce(progress(3, 2, 1))

        mount(BatchPoller, {props: {batchId: 'b-1', intervalMs: 0}})
        await flushPromises()

        expect(mockGet).toHaveBeenCalledWith('/api/v1/batches/b-1')
        expect(mockGet).toHaveBeenCalledTimes(1)

        await new Promise(resolve => setTimeout(resolve, 20))
        expect(mockGet).toHaveBeenCalledTimes(1) // 멈췄으므로 추가 호출 없음
    })

    it('done 이벤트는 최종 진행률을 담아 emit한다', async () => {
        mockGet.mockResolvedValueOnce(progress(3, 3, 0))

        const wrapper = mount(BatchPoller, {props: {batchId: 'b-1', intervalMs: 0}})
        await flushPromises()

        expect(wrapper.emitted('done')).toBeTruthy()
        expect(wrapper.emitted('done')![0]).toEqual([{batchId: 'b-1', totalCount: 3, doneCount: 3, failCount: 0}])
    })

    it('진행 중에는 progress를 emit하며 계속 폴링하다가 완료되면 멈춘다', async () => {
        mockGet
            .mockResolvedValueOnce(progress(3, 1, 0))
            .mockResolvedValueOnce(progress(3, 2, 0))
            .mockResolvedValueOnce(progress(3, 3, 0))

        const wrapper = mount(BatchPoller, {props: {batchId: 'b-1', intervalMs: 0}})

        await vi.waitFor(() => expect(mockGet).toHaveBeenCalledTimes(3))
        await flushPromises()

        // 미완료 응답마다 progress를 흘려보냈다 (완료 응답은 done으로 처리).
        expect(wrapper.emitted('progress')).toBeTruthy()
        expect(wrapper.emitted('progress')!.length).toBe(2)
        expect(wrapper.emitted('done')).toBeTruthy()

        await new Promise(resolve => setTimeout(resolve, 20))
        expect(mockGet).toHaveBeenCalledTimes(3)
    })

    it('totalCount가 0이면 완료로 오판하지 않는다', async () => {
        // done(0)+fail(0)==total(0) 이지만 아직 아무 job도 없다 → 완료가 아니다.
        mockGet
            .mockResolvedValueOnce(progress(0, 0, 0))
            .mockResolvedValueOnce(progress(2, 2, 0))

        const wrapper = mount(BatchPoller, {props: {batchId: 'b-1', intervalMs: 0}})
        await vi.waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2))
        await flushPromises()

        expect(wrapper.emitted('done')).toBeTruthy()
        expect(wrapper.emitted('done')![0]).toEqual([{batchId: 'b-1', totalCount: 2, doneCount: 2, failCount: 0}])
    })
})
