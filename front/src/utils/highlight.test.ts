import {describe, expect, it} from 'vitest'
import {ensureHighlightStyles, escapeHtml, highlightToHtml, resolveLanguage} from './highlight'

describe('resolveLanguage', () => {
    it('등록된 언어는 소문자 정규화된 이름을 반환한다', () => {
        expect(resolveLanguage('sql')).toBe('sql')
        expect(resolveLanguage('SQL')).toBe('sql')
        expect(resolveLanguage('xml')).toBe('xml')
        expect(resolveLanguage('json')).toBe('json')
        expect(resolveLanguage('yaml')).toBe('yaml')
        expect(resolveLanguage('java')).toBe('java')
        expect(resolveLanguage('typescript')).toBe('typescript')
        expect(resolveLanguage('bash')).toBe('bash')
    })

    it('별칭(ts, sh, html)도 해석된다', () => {
        expect(resolveLanguage('ts')).toBe('ts')
        expect(resolveLanguage('sh')).toBe('sh')
        expect(resolveLanguage('html')).toBe('html')
    })

    it('미등록 언어와 빈 값은 null을 반환한다', () => {
        expect(resolveLanguage('cobol')).toBeNull()
        expect(resolveLanguage('')).toBeNull()
        expect(resolveLanguage(undefined)).toBeNull()
    })
})

describe('escapeHtml', () => {
    it('HTML 특수문자를 전부 이스케이프한다', () => {
        expect(escapeHtml(`<a href="x" data-y='1'>b & c</a>`))
            .toBe('&lt;a href=&quot;x&quot; data-y=&#39;1&#39;&gt;b &amp; c&lt;/a&gt;')
    })
})

describe('highlightToHtml', () => {
    it('SQL 키워드를 hljs-keyword 스팬으로 감싼다', () => {
        const html = highlightToHtml('SELECT id FROM users', 'sql')
        // 키워드가 정확히 토큰화되고 식별자는 그대로 남는다.
        expect(html).toContain('<span class="hljs-keyword">SELECT</span>')
        expect(html).toContain('<span class="hljs-keyword">FROM</span>')
        expect(html).toContain('users')
        expect(html).not.toContain('<span class="hljs-keyword">users</span>')
    })

    it('SQL 문자열 리터럴과 숫자를 각각 토큰화한다', () => {
        const html = highlightToHtml("SELECT * FROM t WHERE name = 'kim' LIMIT 10", 'sql')
        expect(html).toContain(`<span class="hljs-string">&#x27;kim&#x27;</span>`)
        expect(html).toContain('<span class="hljs-number">10</span>')
    })

    it('XML 태그명과 속성을 토큰화하고 내용은 이스케이프한다', () => {
        const html = highlightToHtml('<root attr="1"><b>x & y</b></root>', 'xml')
        expect(html).toContain('<span class="hljs-name">root</span>')
        expect(html).toContain('<span class="hljs-attr">attr</span>')
        expect(html).toContain('x &amp; y')
        expect(html).not.toContain('<root') // 원본 태그가 그대로 살아있으면 XSS
    })

    it('미등록 언어는 이스케이프된 원문으로 폴백한다', () => {
        const html = highlightToHtml('<script>alert(1)</script>', 'cobol')
        expect(html).toBe('&lt;script&gt;alert(1)&lt;/script&gt;')
    })

    it('언어 미지정도 이스케이프된 원문으로 폴백한다', () => {
        expect(highlightToHtml('a < b')).toBe('a &lt; b')
    })

    it('빈 코드는 빈 문자열을 반환한다', () => {
        expect(highlightToHtml('', 'sql')).toBe('')
    })
})

describe('ensureHighlightStyles', () => {
    it('스타일을 1회만 주입하고 라이트/다크 변수를 모두 포함한다', () => {
        ensureHighlightStyles()
        ensureHighlightStyles()
        const styles = document.querySelectorAll('#hljs-token-theme')
        expect(styles.length).toBe(1)
        const css = styles[0].textContent ?? ''
        expect(css).toContain(':root')
        expect(css).toContain('.dark')
        expect(css).toContain('.hljs-keyword')
        expect(css).toContain('var(--hl-keyword)')
    })
})
