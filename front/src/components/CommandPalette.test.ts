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

describe('CommandPalette — 결과 그룹핑 (046)', () => {
    it('그룹 헤딩이 "구역명 > 카테고리명" 형식이다', async () => {
        const wrapper = mount(CommandPalette, {props: {modules: MODULES}, global: {plugins: [router]}, attachTo: document.body})
        wrapper.vm.open()
        await wrapper.vm.$nextTick()

        // CommandDialog는 reka-ui DialogPortal로 document.body에 텔레포트되어 wrapper 밖에 렌더된다
        expect(document.body.textContent).toContain('파일·문서 > PDF')
        expect(document.body.textContent).toContain('개발자 도구 > 포맷터')
    })
})
