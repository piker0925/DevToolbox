import {describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import FileResultPanel from './FileResultPanel.vue'
import PdfThumbnail from './PdfThumbnail.vue'

describe('FileResultPanel', () => {
    it('이미지 결과면 <img>와 다운로드 링크를 보여준다', () => {
        const wrapper = mount(FileResultPanel, {
            props: {url: 'http://x/j/result.png', advisory: null},
        })
        expect(wrapper.find('img').attributes('src')).toBe('http://x/j/result.png')
        expect(wrapper.find('a[download]').attributes('href')).toBe('http://x/j/result.png')
    })

    it('pdf 결과면 PdfThumbnail을 렌더하고, emit된 페이지 수를 액션 바에 표시한다', async () => {
        const wrapper = mount(FileResultPanel, {
            props: {url: 'http://x/j/result.pdf', advisory: null},
            global: {stubs: {PdfThumbnail: true}},
        })
        // 로드 전에는 페이지 수를 표시하지 않는다.
        expect(wrapper.text()).not.toContain('총')
        expect(wrapper.find('img').exists()).toBe(false)

        wrapper.findComponent(PdfThumbnail).vm.$emit('pages', 5)
        await wrapper.vm.$nextTick()
        expect(wrapper.text()).toContain('총 5페이지')
    })

    it('결과(url)가 PDF에서 다른 형식으로 바뀌면 이전 PDF의 페이지 수가 잔존하지 않는다', async () => {
        const wrapper = mount(FileResultPanel, {
            props: {url: 'http://x/j/a.pdf', advisory: null},
            global: {stubs: {PdfThumbnail: true}},
        })
        wrapper.findComponent(PdfThumbnail).vm.$emit('pages', 5)
        await wrapper.vm.$nextTick()
        expect(wrapper.text()).toContain('총 5페이지')

        // 이미지 결과로 전환 → 이전 페이지 수가 사라져야 한다(인스턴스 재사용 잔여 방지).
        await wrapper.setProps({url: 'http://x/j/b.png'})
        expect(wrapper.text()).not.toContain('총')
    })

    it('미리보기 불가 형식(zip 등)이면 미리보기 없이 다운로드만 보여준다', () => {
        const wrapper = mount(FileResultPanel, {
            props: {url: 'http://x/j/result.zip', advisory: null},
        })
        expect(wrapper.find('img').exists()).toBe(false)
        expect(wrapper.findComponent(PdfThumbnail).exists()).toBe(false)
        expect(wrapper.find('a[download]').exists()).toBe(true)
    })

    it('advisory 텍스트가 있으면 표시한다 (업스케일 경고 등)', () => {
        const wrapper = mount(FileResultPanel, {
            props: {url: 'http://x/j/result.png', advisory: '업스케일 경고'},
        })
        expect(wrapper.text()).toContain('업스케일 경고')
    })
})
