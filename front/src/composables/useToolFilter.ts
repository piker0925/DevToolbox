import {ref} from 'vue'

const activeCategory = ref<string | null>(null)

export function useToolFilter() {
    return {
        activeCategory,
        setCategory(cat: string | null) {
            activeCategory.value = cat
        },
    }
}
