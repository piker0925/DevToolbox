import {describe, expect, it} from 'vitest'
import {MOCK_MODULES} from './mock'

describe('MOCK_MODULES', () => {
    it('GIF 생성 모듈의 id는 백엔드 GifModule.getId()와 동일한 gif-create여야 한다', () => {
        const gif = MOCK_MODULES.find(m => m.name === 'GIF 생성')
        expect(gif?.id).toBe('gif-create')
    })
})
