import {describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import ResultViewer from './ResultViewer.vue'

describe('ResultViewer', () => {
    it('url이 있으면 다운로드 링크를 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: 'http://example.com/result.pdf', text: null},
        })
        expect(wrapper.find('a[download]').exists()).toBe(true)
        expect(wrapper.find('a[download]').attributes('href')).toBe('http://example.com/result.pdf')
        expect(wrapper.find('textarea').exists()).toBe(false)
    })

    it('text가 있으면 textarea와 복사 버튼을 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: null, text: 'e3b0c442...'},
        })
        expect(wrapper.find('textarea').exists()).toBe(true)
        expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('e3b0c442...')
        expect(wrapper.find('button').exists()).toBe(true)
        expect(wrapper.find('a[download]').exists()).toBe(false)
    })

    it('복사 버튼을 누르면 text 내용을 클립보드에 복사한다', async () => {
        const writeText = vi.fn()
        Object.assign(navigator, {clipboard: {writeText}})

        const wrapper = mount(ResultViewer, {
            props: {url: null, text: 'e3b0c442...'},
        })
        await wrapper.find('button').trigger('click')

        expect(writeText).toHaveBeenCalledWith('e3b0c442...')
    })
})
