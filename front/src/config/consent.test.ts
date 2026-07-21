import {afterEach, describe, expect, it} from 'vitest'
import {getConsent, setConsent} from './consent'

describe('consent', () => {
    afterEach(() => {
        localStorage.clear()
    })

    it('저장한 적 없으면 null을 반환한다', () => {
        expect(getConsent()).toBeNull()
    })

    it('동의를 저장하면 그대로 조회된다', () => {
        setConsent('granted')

        expect(getConsent()).toBe('granted')
    })

    it('거부를 저장하면 그대로 조회된다', () => {
        setConsent('denied')

        expect(getConsent()).toBe('denied')
    })

    it('저장된 값이 granted/denied가 아니면 null을 반환한다', () => {
        localStorage.setItem('ontool-analytics-consent', 'garbage')

        expect(getConsent()).toBeNull()
    })
})
