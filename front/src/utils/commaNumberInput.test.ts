import {describe, expect, it} from 'vitest'
import {ref} from 'vue'
import {useCommaNumberInput} from './commaNumberInput'

describe('useCommaNumberInput', () => {
    it('숫자 3000000을 읽으면 콤마가 붙은 "3,000,000"이 나옴', () => {
        const source = ref(3_000_000)
        const display = useCommaNumberInput(source)
        expect(display.value).toBe('3,000,000')
    })

    it('콤마가 섞인 문자열 "3,000,000"을 쓰면 원본 숫자 ref가 3000000으로 갱신됨', () => {
        const source = ref(0)
        const display = useCommaNumberInput(source)
        display.value = '3,000,000'
        expect(source.value).toBe(3_000_000)
    })

    it('소수점이 있는 "215.688"도 그대로 파싱됨(만원 단위 소수 입력 지원)', () => {
        const source = ref(0)
        const display = useCommaNumberInput(source)
        display.value = '215.688'
        expect(source.value).toBe(215.688)
    })

    it('빈 문자열이나 숫자가 아닌 값을 쓰면 0으로 떨어짐(NaN이 되지 않음)', () => {
        const source = ref(500)
        const display = useCommaNumberInput(source)
        display.value = ''
        expect(source.value).toBe(0)
    })
})
