import {describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import ResultViewer from './ResultViewer.vue'

describe('ResultViewer', () => {
    it('url이 있으면 다운로드 링크를 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: 'http://example.com/result.pdf', text: null},
        })
        expect(wrapper.find('a[download]').exists()).toBe(true)
        expect(wrapper.find('textarea').exists()).toBe(false)
    })

    it('text가 있으면 textarea와 복사 버튼을 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: null, text: 'e3b0c442...'},
        })
        expect(wrapper.find('textarea').exists()).toBe(true)
        expect(wrapper.find('button').exists()).toBe(true)
        expect(wrapper.find('a[download]').exists()).toBe(false)
    })
})
