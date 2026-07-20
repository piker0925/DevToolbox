import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import {ref} from 'vue'
import ZoneHomePage from './ZoneHomePage.vue'
import {apiClient} from '../api/client'
import type {Module} from '../types'

vi.mock('../api/client', () => ({
    apiClient: {get: vi.fn()},
}))

const favoriteIds = ref<string[]>([])
vi.mock('../composables/useFavorites', () => ({
    useFavorites: () => ({favoriteIds, isFavorite: (id: string) => favoriteIds.value.includes(id), toggle: vi.fn()}),
}))

// 실제 MOCK_MODULES(프론트 전용 카탈로그)는 계속 커져서 "이 구역은 항상 비어있다"는 가정이 쉽게 깨진다
// (예: 078에서 fun 구역에 게임이 채워짐). 이 테스트 파일은 프론트 전용 카탈로그를 직접 통제한다.
let mockFrontendOnlyModules: Module[] = []
vi.mock('../api/mock', () => ({
    get MOCK_MODULES() {
        return mockFrontendOnlyModules
    },
}))

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

const router = createRouter({
    history: createMemoryHistory(),
    routes: [{path: '/dev', component: ZoneHomePage}],
})

beforeEach(() => {
    vi.clearAllMocks()
    favoriteIds.value = []
    mockFrontendOnlyModules = []
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

    it('프론트 전용 모듈은 카테고리가 CATEGORY_ORDER에 없어도(예: 생활) 구역 홈 그리드에 실제로 렌더링된다', async () => {
        mockGet.mockResolvedValueOnce({data: []})
        mockFrontendOnlyModules = [
            {id: 'salary-calculator', name: '급여 계산기', category: '생활', isHeavy: false, isFrontendOnly: true, zones: ['life']},
        ]

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'life'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('급여 계산기')
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
        // 이 테스트 파일은 프론트 전용 카탈로그를 직접 통제하므로(mockFrontendOnlyModules 기본값 []),
        // 실제 MOCK_MODULES에 어떤 구역이 채워지든(예: 078의 fun 구역 게임) 이 테스트는 영향받지 않는다.
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
            ],
        })

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'fun'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).toContain('준비 중')
        expect(wrapper.text()).not.toContain('PDF 병합')
    })

    it('즐겨찾기여도 현재 구역 소속이 아니면 노출되지 않는다 — 구역 홈은 카탈로그이지 개인화 위젯이 아님(사이드바가 즐겨찾기 담당)', async () => {
        mockGet.mockResolvedValueOnce({
            data: [
                {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
                {id: 'sql-formatter', name: 'SQL 포맷터', category: '포맷터', isHeavy: false, zones: ['dev']},
            ],
        })
        favoriteIds.value = ['pdf-merge']

        const wrapper = mount(ZoneHomePage, {props: {zoneId: 'dev'}, global: {plugins: [router]}})
        await flushPromises()

        expect(wrapper.text()).not.toContain('PDF 병합')
        expect(wrapper.text()).toContain('SQL 포맷터')
    })
})
