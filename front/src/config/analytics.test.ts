import {afterEach, describe, expect, it, vi} from 'vitest'
import {setConsent} from './consent'

describe('analytics', () => {
    afterEach(() => {
        vi.unstubAllEnvs()
        vi.resetModules()
        localStorage.clear()
        document.head.querySelectorAll('script[src*="googletagmanager"]').forEach(el => el.remove())
        delete window.gtag
        delete window.dataLayer
    })

    it('VITE_GA_ID가 없으면 gtag 스크립트를 주입하지 않는다', async () => {
        vi.stubEnv('VITE_GA_ID', undefined)
        vi.resetModules()
        setConsent('granted')

        const {initAnalytics} = await import('./analytics')
        initAnalytics()

        expect(document.head.querySelector('script[src*="googletagmanager"]')).toBeNull()
    })

    it('VITE_GA_ID가 없으면 trackPageView가 gtag를 호출하지 않는다', async () => {
        vi.stubEnv('VITE_GA_ID', undefined)
        vi.resetModules()

        const {trackPageView} = await import('./analytics')
        window.gtag = vi.fn()
        trackPageView('/dev')

        expect(window.gtag).not.toHaveBeenCalled()
    })

    it('VITE_GA_ID가 있고 아직 결정하지 않았으면(미결정) 기본값으로 gtag 스크립트를 주입한다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()

        const {initAnalytics} = await import('./analytics')
        initAnalytics()

        expect(document.head.querySelector('script[src*="googletagmanager"]')).not.toBeNull()
    })

    it('VITE_GA_ID가 있어도 거부했으면 gtag 스크립트를 주입하지 않는다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()
        setConsent('denied')

        const {initAnalytics} = await import('./analytics')
        initAnalytics()

        expect(document.head.querySelector('script[src*="googletagmanager"]')).toBeNull()
    })

    it('VITE_GA_ID가 있고 동의했으면 gtag 스크립트를 정확한 측정 ID로 주입한다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()
        setConsent('granted')

        const {initAnalytics} = await import('./analytics')
        initAnalytics()

        const script = document.head.querySelector('script[src*="googletagmanager"]')
        expect(script?.getAttribute('src')).toBe('https://www.googletagmanager.com/gtag/js?id=G-TEST123')
    })

    it('VITE_GA_ID가 있으면 trackPageView가 page_view 이벤트로 gtag를 호출한다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()

        const {trackPageView} = await import('./analytics')
        window.gtag = vi.fn()
        trackPageView('/dev')

        expect(window.gtag).toHaveBeenCalledWith('event', 'page_view', {page_path: '/dev'})
    })

    it('initAnalytics를 두 번 호출해도 스크립트가 중복 주입되지 않는다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()

        const {initAnalytics} = await import('./analytics')
        initAnalytics()
        initAnalytics()

        expect(document.head.querySelectorAll('script[src*="googletagmanager"]')).toHaveLength(1)
    })

    it('disableAnalytics는 주입된 스크립트를 제거하고 gtag/dataLayer를 지운다', async () => {
        vi.stubEnv('VITE_GA_ID', 'G-TEST123')
        vi.resetModules()
        setConsent('granted')

        const {initAnalytics, disableAnalytics} = await import('./analytics')
        initAnalytics()
        expect(document.head.querySelector('script[src*="googletagmanager"]')).not.toBeNull()

        disableAnalytics()

        expect(document.head.querySelector('script[src*="googletagmanager"]')).toBeNull()
        expect(window.gtag).toBeUndefined()
        expect(window.dataLayer).toBeUndefined()
    })
})
