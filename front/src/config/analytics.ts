import {getConsent} from './consent'

declare global {
    interface Window {
        dataLayer?: unknown[]
        gtag?: (...args: unknown[]) => void
    }
}

function getGaId(): string | undefined {
    return import.meta.env.VITE_GA_ID as string | undefined
}

export function isAnalyticsConfigured() {
    return Boolean(getGaId())
}

export function initAnalytics() {
    const gaId = getGaId()
    if (!gaId || getConsent() === 'denied') return
    if (document.head.querySelector('script[src*="googletagmanager"]')) return

    const script = document.createElement('script')
    script.async = true
    script.src = `https://www.googletagmanager.com/gtag/js?id=${gaId}`
    document.head.appendChild(script)

    window.dataLayer = window.dataLayer ?? []
    // gtag.js는 dataLayer 항목이 arguments 객체일 때만 커맨드로 인식한다(내부적으로 [object Arguments] 체크).
    // 화살표 함수 (...args) => push(args)로 바꾸면 일반 배열이 되어 이 인식을 통과 못 하고
    // config/event가 전부 조용히 무시된다 — 실측정 자체가 초기화되지 않아 collect가 영원히 안 나감.
    window.gtag = function () {
        window.dataLayer!.push(arguments)
    }
    window.gtag('js', new Date())
    window.gtag('config', gaId)
}

export function disableAnalytics() {
    document.head.querySelectorAll('script[src*="googletagmanager"]').forEach(el => el.remove())
    delete window.gtag
    delete window.dataLayer
}

export function trackPageView(path: string) {
    if (!getGaId()) return
    window.gtag?.('event', 'page_view', {page_path: path})
}
