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

    it('이미지 확장자 url이면 다운로드 링크와 함께 미리보기 이미지를 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: 'http://example.com/files/job-1/result.png', text: null},
        })
        expect(wrapper.find('img').exists()).toBe(true)
        expect(wrapper.find('img').attributes('src')).toBe('http://example.com/files/job-1/result.png')
        expect(wrapper.find('a[download]').exists()).toBe(true)
    })

    it('이미지가 아닌 확장자 url이면 미리보기 없이 다운로드 링크만 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {url: 'http://example.com/files/job-1/result.pdf', text: null},
        })
        expect(wrapper.find('img').exists()).toBe(false)
        expect(wrapper.find('a[download]').exists()).toBe(true)
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
