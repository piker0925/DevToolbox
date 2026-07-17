import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import {ref} from 'vue'
import ZoneHomePage from './ZoneHomePage.vue'
import {apiClient} from '../api/client'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn()},
}))

const favoriteIds = ref<string[]>([])
vi.mock('../composables/useFavorites', () => ({
    useFavorites: () => ({favoriteIds, isFavorite: (id: string) => favoriteIds.value.includes(id), toggle: vi.fn()}),
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

const router = createRouter({
    history: createMemoryHistory(),
    routes: [{path: '/dev', component: ZoneHomePage}],
})

beforeEach(() => {
    vi.clearAllMocks()
    favoriteIds.value = []
})

describe('ZoneHomePage', () => {
    it('현재 구역(zones)에 속한 모듈만 카드로 렌더링한다', async () => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
                {id: 'sql-formatter', name: 'SQL 포맷터', category: '포맷터', isHeavy: false, zones: ['dev']},
            ],
        })

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'dev'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('SQL 포맷터')
        expect(wrapper.text()).not.toContain('PDF 병합')
    })

    it.each(['dev', 'files'] as const)('복수 구역 모듈은 %s 구역 홈에도 노출된다 (양쪽 다 확인)', async (zoneId) => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'exif-strip', name: 'EXIF 제거', category: '이미지', isHeavy: false, zones: ['files', 'dev']},
            ],
        })

        const wrapper = mount(ZoneHomePage, {props: {zoneId}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('EXIF 제거')
    })

    it('해당 구역에 도구가 없으면 준비 중 안내 문구를 보여준다', async () => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
            ],
        })

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'life'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('준비 중')
        expect(wrapper.text()).not.toContain('PDF 병합')
    })

    it('즐겨찾기는 구역과 무관하게 노출된다', async () => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
            ],
        })
        favoriteIds.value = ['pdf-merge']

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'dev'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('PDF 병합')
    })
})
