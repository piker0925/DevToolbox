import {describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import CommandPalette from './CommandPalette.vue'
import type {Module} from '../types'

const router = createRouter({
    history: createMemoryHistory(),
    routes: [{path: '/tools/:moduleId', component: {template: '<div/>'}}],
})

const MODULES: Module[] = [
    {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, zones: ['files']},
    {id: 'sql-formatter', name: 'SQL 포맷터', category: '포맷터', isHeavy: false, zones: ['dev']},
]

describe('CommandPalette — 결과 그룹핑', () => {
    it('그룹 헤딩이 구역명(zones[0]) 단위다 — 카테고리까지 묶지 않아 스캐닝을 극대화한다 (ADR-0023)', async () => {
        const wrapper = mount(CommandPalette, {props: {modules: MODULES}, global: {plugins: [router]}, attachTo: document.body})
        wrapper.vm.open()
        await wrapper.vm.$nextTick()

        // CommandDialog는 reka-ui DialogPortal로 document.body에 텔레포트되어 wrapper 밖에 렌더된다
        expect(document.body.textContent).toContain('파일·문서')
        expect(document.body.textContent).toContain('개발자 도구')
        // 그룹 헤딩 자체는 구역명만이다 — "카테고리명"을 붙인 합성 헤딩(예: "파일·문서 > PDF")은 없다
        expect(document.body.textContent).not.toContain('파일·문서 > PDF')
        expect(document.body.textContent).not.toContain('개발자 도구 > 포맷터')
    })
})
