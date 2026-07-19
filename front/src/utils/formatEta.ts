// ETA 초 → 사람이 읽는 문자열 (예: "45초", "2분 10초")
export function formatEta(seconds: number): string {
    if (seconds < 60) return `${Math.max(1, Math.round(seconds))}초`
    const m = Math.floor(seconds / 60)
    const s = Math.round(seconds % 60)
    return s > 0 ? `${m}분 ${s}초` : `${m}분`
}
