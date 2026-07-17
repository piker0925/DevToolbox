import {describe, expect, it} from 'vitest'
import {buildRobotsTxt, buildSitemapXml} from './sitemap'

const BASE = 'https://ontool.example'

describe('buildSitemapXml', () => {
    it('4개 구역 홈 URL을 모두 포함한다', () => {
        const xml = buildSitemapXml(BASE)

        expect(xml).toContain('<loc>https://ontool.example/dev</loc>')
        expect(xml).toContain('<loc>https://ontool.example/files</loc>')
        expect(xml).toContain('<loc>https://ontool.example/life</loc>')
        expect(xml).toContain('<loc>https://ontool.example/fun</loc>')
    })

    it('모든 도구 상세 URL을 포함한다', () => {
        const xml = buildSitemapXml(BASE)

        expect(xml).toContain('<loc>https://ontool.example/tools/pdf-merge</loc>')
        expect(xml).toContain('<loc>https://ontool.example/tools/uuid</loc>')
    })

    it('유효한 sitemap XML 루트 요소를 갖는다', () => {
        const xml = buildSitemapXml(BASE)

        expect(xml).toContain('<?xml version="1.0" encoding="UTF-8"?>')
        expect(xml).toContain('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">')
        expect(xml).toContain('</urlset>')
    })

    it('baseUrl 끝에 슬래시가 있어도 중복 슬래시 없이 URL을 만든다', () => {
        const xml = buildSitemapXml('https://ontool.example/')

        expect(xml).toContain('<loc>https://ontool.example/dev</loc>')
        expect(xml).not.toContain('//dev')
    })
})

describe('buildRobotsTxt', () => {
    it('전체 허용 + /admin 차단 + sitemap 위치를 포함한다', () => {
        const txt = buildRobotsTxt(BASE)

        expect(txt).toContain('Allow: /')
        expect(txt).toContain('Disallow: /admin')
        expect(txt).toContain('Sitemap: https://ontool.example/sitemap.xml')
    })
})
