export type PreviewKind = 'image' | 'pdf' | 'none'

/**
 * 결과 파일 URL의 확장자로 미리보기 종류를 결정한다.
 * 백엔드 resultKey는 `{jobId}/result.<ext>` 형식이라 확장자 기반 판별이 안정적이다.
 * - image: 브라우저가 바로 그릴 수 있는 이미지 → <img>
 * - pdf: PDF.js로 첫 페이지 썸네일 렌더
 * - none: ZIP 등 미리보기 대상 아님 → 다운로드만
 */
export function previewKind(url: string | null): PreviewKind {
    if (!url) return 'none'
    if (/\.(png|jpe?g|gif|webp)$/i.test(url)) return 'image'
    if (/\.pdf$/i.test(url)) return 'pdf'
    return 'none'
}
