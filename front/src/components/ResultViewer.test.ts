import {describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import ResultViewer from './ResultViewer.vue'

// ResultViewer는 텍스트 결과 전용. 파일(이미지·PDF·ZIP) 미리보기/다운로드는 FileResultPanel 담당.
describe('ResultViewer', () => {
    it('text가 있으면 textarea와 복사 버튼을 보여준다', () => {
        const wrapper = mount(ResultViewer, {
            props: {text: 'e3b0c442...'},
        })
        expect(wrapper.find('textarea').exists()).toBe(true)
        expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('e3b0c442...')
        expect(wrapper.find('button').exists()).toBe(true)
    })

    it('복사 버튼을 누르면 text 내용을 클립보드에 복사한다', async () => {
        const writeText = vi.fn()
        Object.assign(navigator, {clipboard: {writeText}})

        const wrapper = mount(ResultViewer, {
            props: {text: 'e3b0c442...'},
        })
        await wrapper.find('button').trigger('click')

        expect(writeText).toHaveBeenCalledWith('e3b0c442...')
    })

    it('text가 없으면 아무것도 렌더하지 않는다', () => {
        const wrapper = mount(ResultViewer, {
            props: {text: null},
        })
        expect(wrapper.find('textarea').exists()).toBe(false)
        expect(wrapper.find('button').exists()).toBe(false)
    })
})
