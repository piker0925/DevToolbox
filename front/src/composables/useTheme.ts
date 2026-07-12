import {computed, ref, watchEffect} from 'vue'

export type ThemePreference = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'devtoolbox-theme'

function load(): ThemePreference {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved === 'light' || saved === 'dark' || saved === 'system' ? saved : 'system'
}

// jsdom(테스트 환경)에는 matchMedia가 없을 수 있다
const media = typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: dark)')
    : null

const preference = ref<ThemePreference>(load())
const systemDark = ref(media?.matches ?? false)

media?.addEventListener('change', e => {
    systemDark.value = e.matches
})

const isDark = computed(() =>
    preference.value === 'dark' || (preference.value === 'system' && systemDark.value),
)

watchEffect(() => {
    document.documentElement.classList.toggle('dark', isDark.value)
})

export function useTheme() {
    return {
        preference,
        isDark,
        setTheme(next: ThemePreference) {
            preference.value = next
            localStorage.setItem(STORAGE_KEY, next)
        },
    }
}
