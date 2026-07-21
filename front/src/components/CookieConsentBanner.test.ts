import {afterEach, describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import CookieConsentBanner from './CookieConsentBanner.vue'
import {getConsent, setConsent} from '../config/consent'
import {initAnalytics} from '../config/analytics'

afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
    localStorage.clear()
    document.head.querySelectorAll('script[src*="googletagmanager"]').forEach(el => el.remove())
    delete window.gtag
    delete window.dataLayer
})

describe('CookieConsentBanner', () => {
    it('GA_ID가 설정되어 있지 않으면 배너를 렌더링하지 않는다', () => {
        vi.stubEnv('VITE_GA_ID', undefined)

        const wrapper = mount(CookieConsentBanner)

        expect(wrapper.text()).toBe('')
    })

    it('GA_ID가 있고 아직 결정하지 않았으면 배너를 보여준다', () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')

        const wrapper = mount(CookieConsentBanner)

        expect(wrapper.text()).toContain('Google Analytics')
    })

    it('이미 동의했으면 배너를 보여주지 않는다', () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        setConsent('granted')

        const wrapper = mount(CookieConsentBanner)

        expect(wrapper.text()).toBe('')
    })

    it('이미 거부했으면 배너를 보여주지 않는다', () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        setConsent('denied')

        const wrapper = mount(CookieConsentBanner)

        expect(wrapper.text()).toBe('')
    })

    it('동의 버튼을 누르면 동의가 저장되고 배너가 사라지고 gtag가 주입된다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')

        const wrapper = mount(CookieConsentBanner)
        await wrapper.find('[data-testid="consent-accept"]').trigger('click')

        expect(getConsent()).toBe('granted')
        expect(wrapper.text()).toBe('')
        expect(document.head.querySelector('script[src*="googletagmanager"]')).not.toBeNull()
    })

    it('거부 버튼을 누르면 거부가 저장되고 배너가 사라지고 gtag는 주입되지 않는다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')

        const wrapper = mount(CookieConsentBanner)
        await wrapper.find('[data-testid="consent-decline"]').trigger('click')

        expect(getConsent()).toBe('denied')
        expect(wrapper.text()).toBe('')
        expect(document.head.querySelector('script[src*="googletagmanager"]')).toBeNull()
    })

    it('기본 허용으로 이미 로드된 상태에서 거부하면 로드된 스크립트를 제거한다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        initAnalytics()
        expect(document.head.querySelector('script[src*="googletagmanager"]')).not.toBeNull()

        const wrapper = mount(CookieConsentBanner)
        await wrapper.find('[data-testid="consent-decline"]').trigger('click')

        expect(document.head.querySelector('script[src*="googletagmanager"]')).toBeNull()
        expect(window.gtag).toBeUndefined()
    })
})
