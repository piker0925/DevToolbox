import {ZONES} from '../config/zones.js'
import {MOCK_MODULES} from '../api/mock.js'

function buildSitemapUrls(baseUrl: string): string[] {
    const base = baseUrl.replace(/\/$/, '')
    const staticPaths = ['/', ...ZONES.map(z => z.route), '/suggestions']
    const toolPaths = MOCK_MODULES.map(m => `/tools/${m.id}`)
    return [...staticPaths, ...toolPaths].map(p => `${base}${p}`)
}

export function buildSitemapXml(baseUrl: string): string {
    const entries = buildSitemapUrls(baseUrl).map(u => `  <url><loc>${u}</loc></url>`).join('\n')
    return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries}\n</urlset>\n`
}

export function buildRobotsTxt(baseUrl: string): string {
    const base = baseUrl.replace(/\/$/, '')
    return `User-agent: *\nAllow: /\nDisallow: /admin\n\nSitemap: ${base}/sitemap.xml\n`
}
