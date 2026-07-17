import {describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import PrivacyPage from './PrivacyPage.vue'

vi.mock('../config/analytics', () => ({trackPageView: vi.fn()}))

describe('PrivacyPage', () => {
    it('제목과 시행일을 보여준다', () => {
        const wrapper = mount(PrivacyPage)

        expect(wrapper.text()).toContain('개인정보처리방침')
        expect(wrapper.text()).toContain('시행일: 2026-07-17')
    })

    it('수집 항목을 안내한다 — 소셜 로그인은 아직 도입되지 않아 예정으로 표기한다', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('소셜 로그인')
        expect(text).toContain('예정')
        expect(text).toContain('작업 이력')
        expect(text).toContain('GA4')
        expect(text).toContain('서버 로그')
    })

    it('보유 기간을 안내한다 — 탈퇴 시 파기(예정), 업로드 파일은 TTL 자동 삭제', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('탈퇴')
        expect(text).toContain('파기')
        expect(text).toContain('업로드')
        expect(text).toContain('TTL')
        expect(text).toContain('자동')
    })

    it('개정 이력을 시행일과 함께 보여준다', () => {
        const wrapper = mount(PrivacyPage)
        const text = wrapper.text()

        expect(text).toContain('개정 이력')
        expect(text).toContain('2026-07-17')
        expect(text).toContain('최초 시행')
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
