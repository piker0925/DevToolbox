import hljs from 'highlight.js/lib/core'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import yaml from 'highlight.js/lib/languages/yaml'
import java from 'highlight.js/lib/languages/java'
import typescript from 'highlight.js/lib/languages/typescript'
import bash from 'highlight.js/lib/languages/bash'

/**
 * highlight.js 캡슐화 유틸.
 * - core 빌드 + 필요한 언어만 등록해 번들 크기를 최소화한다.
 * - 토큰 색은 CSS 변수 기반 테마(라이트/다크 자동 대응)를 1회 <style> 주입으로 제공한다.
 */

const LANGUAGES: Record<string, Parameters<typeof hljs.registerLanguage>[1]> = {
    sql, xml, json, yaml, java, typescript, bash,
}

for (const [name, definition] of Object.entries(LANGUAGES)) {
    if (!hljs.getLanguage(name)) hljs.registerLanguage(name, definition)
}

/** 등록된 언어(별칭 포함)면 정규화된 소문자 이름을, 아니면 null을 반환 */
export function resolveLanguage(language?: string): string | null {
    if (!language) return null
    const lang = language.toLowerCase()
    return hljs.getLanguage(lang) ? lang : null
}

export function escapeHtml(text: string): string {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
}

/**
 * 코드를 하이라이팅된 HTML 문자열로 변환한다.
 * 미등록 언어이거나 하이라이팅에 실패하면 HTML 이스케이프된 원문을 반환한다 (v-html에 안전).
 */
export function highlightToHtml(code: string, language?: string): string {
    if (!code) return ''
    const lang = resolveLanguage(language)
    if (lang) {
        try {
            return hljs.highlight(code, {language: lang, ignoreIllegals: true}).value
        } catch {
            // 폴백으로 진행
        }
    }
    return escapeHtml(code)
}

const STYLE_ID = 'hljs-token-theme'

/**
 * hljs 토큰 색 테마를 <head>에 1회 주입한다.
 * 색은 CSS 변수로 정의되어 .dark 루트 클래스에 따라 자동 전환된다.
 */
export function ensureHighlightStyles(): void {
    if (typeof document === 'undefined') return
    if (document.getElementById(STYLE_ID)) return
    const style = document.createElement('style')
    style.id = STYLE_ID
    style.textContent = `
:root {
  --hl-comment: oklch(0.55 0.02 262);
  --hl-keyword: oklch(0.47 0.19 257);
  --hl-string: oklch(0.5 0.13 150);
  --hl-number: oklch(0.55 0.15 60);
  --hl-title: oklch(0.48 0.16 300);
  --hl-attr: oklch(0.5 0.12 220);
  --hl-literal: oklch(0.52 0.17 20);
}
.dark {
  --hl-comment: oklch(0.62 0.015 262);
  --hl-keyword: oklch(0.75 0.13 257);
  --hl-string: oklch(0.76 0.12 150);
  --hl-number: oklch(0.78 0.13 70);
  --hl-title: oklch(0.78 0.12 300);
  --hl-attr: oklch(0.78 0.1 220);
  --hl-literal: oklch(0.72 0.14 20);
}
.hljs-comment, .hljs-quote { color: var(--hl-comment); font-style: italic; }
.hljs-keyword, .hljs-selector-tag, .hljs-built_in, .hljs-type, .hljs-name, .hljs-doctag { color: var(--hl-keyword); }
.hljs-string, .hljs-regexp, .hljs-template-tag, .hljs-addition { color: var(--hl-string); }
.hljs-number, .hljs-symbol, .hljs-bullet, .hljs-meta, .hljs-link { color: var(--hl-number); }
.hljs-title, .hljs-section, .hljs-function { color: var(--hl-title); }
.hljs-attr, .hljs-attribute, .hljs-variable, .hljs-template-variable, .hljs-params, .hljs-property { color: var(--hl-attr); }
.hljs-literal, .hljs-deletion { color: var(--hl-literal); }
.hljs-emphasis { font-style: italic; }
.hljs-strong { font-weight: 600; }
`
    document.head.appendChild(style)
}
