const CONSENT_KEY = 'ontool-analytics-consent'

export type ConsentStatus = 'granted' | 'denied'

export function getConsent(): ConsentStatus | null {
    const value = localStorage.getItem(CONSENT_KEY)
    return value === 'granted' || value === 'denied' ? value : null
}

export function setConsent(status: ConsentStatus) {
    localStorage.setItem(CONSENT_KEY, status)
}
