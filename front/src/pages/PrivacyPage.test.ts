import {afterEach, describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import PrivacyPage from './PrivacyPage.vue'
import {setConsent} from '../config/consent'

vi.mock('../config/analytics', () => ({
    trackPageView: vi.fn(),
    initAnalytics: vi.fn(),
    disableAnalytics: vi.fn(),
}))

import {initAnalytics, disableAnalytics} from '../config/analytics'

afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
})

describe('PrivacyPage', () => {
    it('제목과 시행일을 보여준다', () => {
        const wrapper = mount(PrivacyPage)

        expect(wrapper.text()).toContain('개인정보처리방침')
        expect(wrapper.text()).toContain('시행일: 2026-07-21')
    })

    it('수집 항목을 안내한다 — 소셜 로그인은 이미 제공 중인 기능으로 표기한다', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('소셜 로그인')
        expect(text).toContain('구글')
        expect(text).toContain('카카오')
        expect(text).not.toContain('예정')
        expect(text).toContain('작업 이력')
        expect(text).toContain('GA4')
        expect(text).toContain('서버 로그')
    })

    it('보유 기간을 안내한다 — 탈퇴는 이미 제공 중인 기능으로 표기하고, 업로드 파일은 TTL 자동 삭제', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('탈퇴')
        expect(text).toContain('파기')
        expect(text).toContain('익명화')
        expect(text).not.toContain('아직 제공되지 않')
        expect(text).toContain('업로드')
        expect(text).toContain('TTL')
        expect(text).toContain('자동')
    })

    it('개정 이력을 시행일과 함께 보여준다', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('개정 이력')
        expect(text).toContain('2026-07-21')
        expect(text).toContain('2026-07-17')
        expect(text).toContain('최초 시행')
    })

    it('아직 결정하지 않았으면(기본 허용) "허용 취소" 버튼을 보여준다', () => {
        const wrapper = mount(PrivacyPage)

        expect(wrapper.find('[data-testid="consent-toggle"]').text()).toContain('취소')
    })

    it('거부한 상태면 "허용" 버튼을 보여준다', () => {
        setConsent('denied')
        const wrapper = mount(PrivacyPage)

        expect(wrapper.find('[data-testid="consent-toggle"]').text()).toContain('허용하기')
    })

    it('동의한 상태면 "허용 취소" 버튼을 보여준다', () => {
        setConsent('granted')
        const wrapper = mount(PrivacyPage)

        expect(wrapper.find('[data-testid="consent-toggle"]').text()).toContain('취소')
    })

    it('미결정(기본 허용) 상태에서 버튼을 누르면 거부로 저장하고 disableAnalytics를 호출한다', async () => {
        const wrapper = mount(PrivacyPage)

        await wrapper.find('[data-testid="consent-toggle"]').trigger('click')

        const {getConsent} = await import('../config/consent')
        expect(getConsent()).toBe('denied')
        expect(disableAnalytics).toHaveBeenCalledOnce()
        expect(initAnalytics).not.toHaveBeenCalled()
    })

    it('동의한 상태에서 버튼을 누르면 거부로 저장하고 disableAnalytics를 호출한다', async () => {
        setConsent('granted')
        const wrapper = mount(PrivacyPage)

        await wrapper.find('[data-testid="consent-toggle"]').trigger('click')

        const {getConsent} = await import('../config/consent')
        expect(getConsent()).toBe('denied')
        expect(disableAnalytics).toHaveBeenCalledOnce()
        expect(initAnalytics).not.toHaveBeenCalled()
    })

    it('거부한 상태에서 버튼을 누르면 동의로 저장하고 initAnalytics를 호출한다', async () => {
        setConsent('denied')
        const wrapper = mount(PrivacyPage)

        await wrapper.find('[data-testid="consent-toggle"]').trigger('click')

        const {getConsent} = await import('../config/consent')
        expect(getConsent()).toBe('granted')
        expect(initAnalytics).toHaveBeenCalledOnce()
        expect(disableAnalytics).not.toHaveBeenCalled()
    })

    it('/privacy 라우트가 로그인 가드 없이 등록되어 있고 title이 개인정보처리방침으로 바뀐다', async () => {
        const {router} = await import('../router')

        const routes = router.getRoutes()
        const privacyRoute = routes.find(r => r.path === '/privacy')
        expect(privacyRoute).toBeDefined()
        expect(privacyRoute?.beforeEnter).toBeUndefined()

        await router.push('/privacy')
        expect(document.title).toBe('개인정보처리방침 · OnTool')
    })
})
