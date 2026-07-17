import {describe, expect, it} from 'vitest'
import {detectFormat} from './unifiedPageUtils'

describe('detectFormat', () => {
    it('JSON 객체/배열을 감지한다', () => {
        expect(detectFormat('{"name": "OnTool", "active": true}')).toBe('json')
        expect(detectFormat('  [1, 2, 3]')).toBe('json')
    })

    it('중괄호로 시작해도 파싱 불가능하면 null', () => {
        expect(detectFormat('{name: broken')).toBeNull()
    })

    it('XML을 감지한다', () => {
        expect(detectFormat('<root><name>OnTool</name></root>')).toBe('xml')
        expect(detectFormat('<?xml version="1.0"?>\n<a/>')).toBe('xml')
    })

    it('TOML을 감지한다 (key = value, [section])', () => {
        expect(detectFormat('name = "OnTool"\ntags = ["dev", "tools"]')).toBe('toml')
        expect(detectFormat('[server]\nhost = "localhost"\nport = 8080')).toBe('toml')
    })

    it('YAML을 감지한다 (key: value, 리스트)', () => {
        expect(detectFormat('name: OnTool\ntags:\n  - dev\n  - tools\nactive: true')).toBe('yaml')
        expect(detectFormat('host: localhost')).toBe('yaml')
    })

    it('CSV를 감지한다 (행별 콤마 개수 동일)', () => {
        expect(detectFormat('name,age\nAlice,30\nBob,25')).toBe('csv')
    })

    it('행별 콤마 개수가 다르면 CSV로 판단하지 않는다', () => {
        expect(detectFormat('name,age\nAlice,30,extra')).toBeNull()
    })

    it('빈 입력·판단 불가 텍스트는 null', () => {
        expect(detectFormat('')).toBeNull()
        expect(detectFormat('   \n  ')).toBeNull()
        expect(detectFormat('just some plain text')).toBeNull()
    })
})
