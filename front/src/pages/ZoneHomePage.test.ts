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

// 실제 카탈로그(MOCK_MODULES)가 커질수록 "이 구역은 프론트 전용 도구가 하나도 없다"는 실제 데이터로는
// 재현이 불가능해진다 — 빈 구역 안내 문구 테스트만 이 override로 카탈로그를 비운다.
// var 사용: vi.mock 팩토리는 호이스팅되어 ZoneHomePage.vue → api/modules.ts의 import 그래프
// 평가 시점(이 파일의 다른 최상단 코드가 실행되기 전)에 이미 getter가 호출된다.
// let/const면 그 시점에 TDZ ReferenceError가 난다 — var는 undefined로 초기화되어 안전하다.
var mockModulesOverride: Module[] | null = null
vi.mock('../api/mock', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../api/mock')>()
    return {
        get MOCK_MODULES() {
            return mockModulesOverride ?? actual.MOCK_MODULES
        },
    }
})

const mockGet = apiClient.get as ReturnType<typeof vi.fn>

const router = createRouter({
    history: createMemoryHistory(),
    routes: [{path: '/dev', component: ZoneHomePage}],
})

beforeEach(() => {
    vi.clearAllMocks()
    favoriteIds.value = []
    mockModulesOverride = null
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
        // 실카탈로그는 이제 네 구역 모두 프론트 전용 도구를 갖고 있어 빈 구역을 재현할 수 없다 — 카탈로그를 override로 비운다
        mockModulesOverride = []
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
