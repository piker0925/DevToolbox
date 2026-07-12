import {ref} from 'vue'

const STORAGE_KEY = 'devtoolbox-recent'
const MAX_RECENT = 6

function load(): string[] {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    } catch {
        return []
    }
}

const ids = ref<string[]>(load())

export function useRecentTools() {
    return {
        recentIds: ids,
        record(id: string) {
            ids.value = [id, ...ids.value.filter(i => i !== id)].slice(0, MAX_RECENT)
            localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value))
        },
    }
}
