import {computed, ref} from 'vue'

const STORAGE_KEY = 'devtoolbox-likes'

function load(): string[] {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    } catch {
        return []
    }
}

const ids = ref<string[]>(load())
const idSet = computed(() => new Set(ids.value))

function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value))
}

/** 브라우저 단위 좋아요 상태 — 도구당 1회 토글, 새로고침 후에도 유지 */
export function useLikes() {
    return {
        likeIds: ids,
        isLiked: (id: string) => idSet.value.has(id),
        markLiked(id: string) {
            if (!idSet.value.has(id)) {
                ids.value = [...ids.value, id]
                persist()
            }
        },
        markUnliked(id: string) {
            if (idSet.value.has(id)) {
                ids.value = ids.value.filter(i => i !== id)
                persist()
            }
        },
        syncFromServer(newIds: string[]) {
            ids.value = newIds
            persist()
        }
    }
}
