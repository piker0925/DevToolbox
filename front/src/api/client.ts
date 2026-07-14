import axios from 'axios'

// 익명 사용자 식별자 (ADR-0019): 로그인 없이 공정 스케줄링·쿼터의 "버킷" 역할.
// 교차 사이트(Vercel↔OCI)에서 서드파티 쿠키 차단을 피하려고, 쿠키 대신 localStorage 토큰을
// X-Client-Id 헤더로 보낸다. 인증 비밀이 아니라 익명 버킷이므로 JS 가독성은 문제되지 않는다.
function clientId(): string {
    try {
        const KEY = 'dtk_cid'
        let id = localStorage.getItem(KEY)
        if (!id) {
            id = crypto.randomUUID()
            localStorage.setItem(KEY, id)
        }
        return id
    } catch {
        return crypto.randomUUID()
    }
}

export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
    headers: {'X-Client-Id': clientId()},
})
