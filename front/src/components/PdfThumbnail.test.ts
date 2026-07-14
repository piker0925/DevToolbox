import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import PdfThumbnail from './PdfThumbnail.vue'

// pdfjs-dist는 지연 로딩(await import)된다. 목으로 대체해 렌더 배선만 검증하고,
// 실제 캔버스 픽셀은 브라우저에서 확인한다(jsdom은 canvas·PDF.js 실렌더 불가).
const {mockGetDocument} = vi.hoisted(() => ({mockGetDocument: vi.fn()}))
vi.mock('pdfjs-dist', () => ({
    GlobalWorkerOptions: {workerSrc: ''},
    getDocument: mockGetDocument,
}))
// 워커도 지연 import(?url)로 로드된다 — 테스트에선 URL 문자열만 대체한다.
vi.mock('pdfjs-dist/build/pdf.worker.min.mjs?url', () => ({default: 'blob:worker'}))

function fakeDoc(numPages: number) {
    return {
        promise: Promise.resolve({
            numPages,
            getPage: () => Promise.resolve({
                getViewport: () => ({width: 120, height: 160}),
                render: () => ({promise: Promise.resolve()}),
            }),
        }),
    }
}

beforeEach(() => {
    vi.clearAllMocks()
    // jsdom은 canvas 2d 컨텍스트를 구현하지 않아 null을 반환한다.
    // 컴포넌트의 null 가드를 통과시켜 렌더 배선을 테스트하기 위한 최소 스텁.
    ;(HTMLCanvasElement.prototype as unknown as { getContext: () => object }).getContext = () => ({})
})

describe('PdfThumbnail', () => {
    it('첫 페이지 canvas를 렌더하고 총 페이지 수를 pages로 emit한다 (여러 페이지)', async () => {
        mockGetDocument.mockReturnValue(fakeDoc(12))

        const wrapper = mount(PdfThumbnail, {props: {url: 'http://x/j/result.pdf'}})
        await flushPromises()
        await flushPromises()

        expect(mockGetDocument).toHaveBeenCalledWith({url: 'http://x/j/result.pdf'})
        expect(wrapper.find('canvas').exists()).toBe(true)
        // 페이지 수는 부모(액션 바)가 표시하도록 emit한다.
        expect(wrapper.emitted('pages')?.[0]).toEqual([12])
    })

    it('단일 페이지 PDF면 pages로 1을 emit한다', async () => {
        mockGetDocument.mockReturnValue(fakeDoc(1))

        const wrapper = mount(PdfThumbnail, {props: {url: 'http://x/j/one.pdf'}})
        await flushPromises()
        await flushPromises()

        expect(wrapper.find('canvas').exists()).toBe(true)
        expect(wrapper.emitted('pages')?.[0]).toEqual([1])
    })

    it('PDF 로드/렌더가 실패하면 오류 메시지를 보여주고 canvas·pages emit이 없다', async () => {
        mockGetDocument.mockReturnValue({promise: Promise.reject(new Error('bad pdf'))})

        const wrapper = mount(PdfThumbnail, {props: {url: 'http://x/j/broken.pdf'}})
        await flushPromises()
        await flushPromises()

        expect(wrapper.text()).toContain('미리보기를 불러오지 못했습니다')
        expect(wrapper.find('canvas').exists()).toBe(false)
        expect(wrapper.emitted('pages')).toBeFalsy()
    })
})
