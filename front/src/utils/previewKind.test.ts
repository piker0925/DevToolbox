import {describe, expect, it} from 'vitest'
import {previewKind} from './previewKind'

// 결과 URL 확장자로 미리보기 종류를 결정한다 (백엔드 resultKey는 `{jobId}/result.<ext>` 형식).
describe('previewKind — 결과 URL의 미리보기 종류 판별', () => {
    it('이미지 확장자는 image로 판별한다', () => {
        expect(previewKind('http://x/files/j/result.png')).toBe('image')
        expect(previewKind('http://x/files/j/result.jpg')).toBe('image')
        expect(previewKind('http://x/files/j/result.jpeg')).toBe('image')
        expect(previewKind('http://x/files/j/result.gif')).toBe('image')
        expect(previewKind('http://x/files/j/result.webp')).toBe('image')
    })

    it('pdf 확장자는 pdf로 판별한다 (대소문자 무관)', () => {
        expect(previewKind('http://x/files/j/result.pdf')).toBe('pdf')
        expect(previewKind('http://x/files/j/result.PDF')).toBe('pdf')
    })

    it('미리보기 대상이 아닌 확장자·null은 none으로 판별한다', () => {
        // ZIP(pdf-split/codegen)·기타는 미리보기 없이 다운로드만 — pdf로 오판하면 안 된다.
        expect(previewKind('http://x/files/j/result.zip')).toBe('none')
        expect(previewKind('http://x/files/j/result.txt')).toBe('none')
        expect(previewKind(null)).toBe('none')
    })
})
