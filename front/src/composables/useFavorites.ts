import {computed, ref} from 'vue'

const STORAGE_KEY = 'devtoolbox-favorites'

function load(): string[] {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    } catch {
        return []
    }
}

const ids = ref<string[]>(load())
const idSet = computed(() => new Set(ids.value))

export function useFavorites() {
    return {
        favoriteIds: ids,
        isFavorite: (id: string) => idSet.value.has(id),
        toggle(id: string) {
            ids.value = idSet.value.has(id)
                ? ids.value.filter(i => i !== id)
                : [...ids.value, id]
            localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value))
        },
    }
}
