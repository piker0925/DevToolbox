import {describe, expect, it} from 'vitest'
import {previewSplitFileNames} from './pdfSplitPreview'

describe('previewSplitFileNames', () => {
    it('빈 범위 + 낱장 → 전체 페이지가 각각 파일이 된다', () => {
        expect(previewSplitFileNames('', '낱장', 3, 'input.pdf')).toEqual([
            'input-001.pdf', 'input-002.pdf', 'input-003.pdf',
        ])
    })

    it('범위 지정 시 해당 페이지만 낱장으로 나온다', () => {
        expect(previewSplitFileNames('1-2,4', '낱장', 5, 'input.pdf')).toEqual([
            'input-001.pdf', 'input-002.pdf', 'input-004.pdf',
        ])
    })

    it('동일 범위라도 구간 모드면 다른 결과를 낸다', () => {
        expect(previewSplitFileNames('1-2,4', '구간', 5, 'input.pdf')).toEqual([
            'input-001-002.pdf', 'input-004.pdf',
        ])
    })

    it('열린 범위(7-)는 총 페이지까지 확장된다', () => {
        expect(previewSplitFileNames('4-', '낱장', 5, 'input.pdf')).toEqual([
            'input-004.pdf', 'input-005.pdf',
        ])
    })

    it('끝 페이지가 총 페이지를 초과하면 클램프된다', () => {
        expect(previewSplitFileNames('2-100', '구간', 3, 'input.pdf')).toEqual(['input-002-003.pdf'])
    })

    it('겹치는 페이지는 한 번만 나온다', () => {
        expect(previewSplitFileNames('1-2,2-3', '낱장', 3, 'input.pdf')).toEqual([
            'input-001.pdf', 'input-002.pdf', 'input-003.pdf',
        ])
    })

    it('원본 파일명을 접두어로 사용한다', () => {
        expect(previewSplitFileNames('', '낱장', 2, 'report.PDF')).toEqual([
            'report-001.pdf', 'report-002.pdf',
        ])
    })

    it.each([
        ['abc'], ['1,,2'], ['1-2-3'], ['3-2'], ['0-2'], ['9'],
    ])('잘못된 입력 "%s"은 미리보기를 비운다(null)', (spec) => {
        expect(previewSplitFileNames(spec, '낱장', 5, 'input.pdf')).toBeNull()
    })

    it('페이지 수를 아직 모르면(totalPages<1) null', () => {
        expect(previewSplitFileNames('', '낱장', 0, 'input.pdf')).toBeNull()
    })
})
