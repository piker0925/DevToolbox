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
    window.gtag = (...args: unknown[]) => {
        window.dataLayer!.push(args)
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
