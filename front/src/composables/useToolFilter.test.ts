import {describe, expect, it} from 'vitest'
import {defineComponent, h} from 'vue'
import {mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import {useToolFilter} from './useToolFilter'

const Host = defineComponent({
    setup() {
        return {...useToolFilter()}
    },
    render() {
        return h('div', this.activeCategory ?? '')
    },
})

describe('useToolFilter', () => {
    it('setCategory는 현재 경로(구역)를 유지한 채 category 쿼리만 바꾼다', async () => {
        const router = createRouter({
            history: createMemoryHistory(),
            routes: [{path: '/files', component: Host}, {path: '/dev', component: Host}],
        })
        await router.push('/files')

        const wrapper = mount(Host, {global: {plugins: [router]}})
        await wrapper.vm.setCategory('PDF')
        await router.isReady()

        expect(router.currentRoute.value.path).toBe('/files')
        expect(router.currentRoute.value.query.category).toBe('PDF')
    })
})
