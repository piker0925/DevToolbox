import {describe, expect, it, vi} from 'vitest'
import {router} from './index'
import {trackPageView} from '../config/analytics'

vi.mock('../config/analytics', () => ({trackPageView: vi.fn()}))

describe('router', () => {
    it('/ 라우트가 존재한다', () => {
        const routes = router.getRoutes()
        expect(routes.some(r => r.path === '/')).toBe(true)
    })

    it('/tools/:moduleId 라우트가 존재한다', () => {
        const routes = router.getRoutes()
        expect(routes.some(r => r.path === '/tools/:moduleId')).toBe(true)
    })

    it('/tools/qr-code 는 통합 코드 생성기 QR 형식으로 리다이렉트한다', async () => {
        const resolved = router.resolve('/tools/qr-code')
        expect(resolved.matched[0]?.redirect).toBe('/tools/code-gen?format=qr')
    })

    it('/tools/barcode 는 통합 코드 생성기 Code128 형식으로 리다이렉트한다', async () => {
        const resolved = router.resolve('/tools/barcode')
        expect(resolved.matched[0]?.redirect).toBe('/tools/code-gen?format=code128')
    })

    it.each(['/dev', '/files', '/life', '/fun'])('%s 구역 라우트가 존재한다', (zonePath) => {
        const routes = router.getRoutes()
        expect(routes.some(r => r.path === zonePath)).toBe(true)
    })

    it('/ 는 /dev로 리다이렉트한다', () => {
        const resolved = router.resolve('/')
        expect(resolved.matched[0]?.redirect).toBe('/dev')
    })

    it('구역 홈으로 이동하면 title이 구역명 + 사이트명이 되고 meta description이 구역 설명으로 바뀐다', async () => {
        await router.push('/files')

        expect(document.title).toBe('파일·문서 · OnTool')
        expect(document.querySelector('meta[name="description"]')?.getAttribute('content'))
            .toBe('이미지·PDF 등 파일·문서 처리 도구')
    })

    it('도구 페이지로 이동하면 title이 도구명 + 사이트명이 되고 meta description이 도구 설명으로 바뀐다', async () => {
        await router.push('/tools/pdf-merge')

        expect(document.title).toBe('PDF 병합 · OnTool')
        expect(document.querySelector('meta[name="description"]')?.getAttribute('content'))
            .toBe('여러 PDF를 하나로 병합')
    })

    it('라우트 전환마다 trackPageView를 호출한다 (GA4 page_view)', async () => {
        await router.push('/life')

        expect(trackPageView).toHaveBeenCalledWith('/life')
    })
})
