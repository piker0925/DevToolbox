declare global {
    interface Window {
        dataLayer?: unknown[]
        gtag?: (...args: unknown[]) => void
    }
}

const GA_ID = import.meta.env.VITE_GA_ID as string | undefined

export function initAnalytics() {
    if (!GA_ID) return

    const script = document.createElement('script')
    script.async = true
    script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_ID}`
    document.head.appendChild(script)

    window.dataLayer = window.dataLayer ?? []
    window.gtag = (...args: unknown[]) => {
        window.dataLayer!.push(args)
    }
    window.gtag('js', new Date())
    window.gtag('config', GA_ID)
}

export function trackPageView(path: string) {
    if (!GA_ID) return
    window.gtag?.('event', 'page_view', {page_path: path})
}
