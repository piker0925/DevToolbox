import {computed, type Ref} from 'vue'

/**
 * 숫자 ref를 콤마 포맷 문자열로 주고받는 v-model용 computed. 큰 금액 입력칸(연봉·시급 등)에서
 * 타이핑 중에도 천단위 콤마가 보이도록 type="text" input과 함께 쓴다.
 */
export function useCommaNumberInput(source: Ref<number>) {
    return computed<string>({
        get: () => source.value.toLocaleString(),
        set: (raw: string) => {
            const cleaned = raw.replace(/[^0-9.]/g, '')
            const parsed = parseFloat(cleaned)
            source.value = isNaN(parsed) ? 0 : parsed
        },
    })
}
