import {describe, expect, it} from 'vitest'
import {buildFallbackParams} from './lightParams'

describe('buildFallbackParams', () => {
    it('일반 텍스트 입력 시 input/text 키에 그대로 담는다', () => {
        expect(buildFallbackParams('hello')).toEqual({input: 'hello', text: 'hello'})
    })

    it('숫자 형태 문자열도 text 키를 포함한 객체로 감싼다', () => {
        expect(buildFallbackParams('12345')).toEqual({input: '12345', text: '12345'})
    })

    it('JSON으로 파싱 가능한 boolean·배열·객체 형태 문자열도 text 키를 포함한 객체로 감싼다', () => {
        expect(buildFallbackParams('true')).toEqual({input: 'true', text: 'true'})
        expect(buildFallbackParams('[1,2,3]')).toEqual({input: '[1,2,3]', text: '[1,2,3]'})
        expect(buildFallbackParams('{"a":1}')).toEqual({input: '{"a":1}', text: '{"a":1}'})
    })
})
