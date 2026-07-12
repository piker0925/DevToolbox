import {describe, expect, it} from 'vitest'
import {
    captureGroupNames,
    findMatches,
    REGEX_PRESETS,
    replacePreview,
    segmentText,
} from './regexTester'

describe('findMatches', () => {
    it('g 플래그: 모든 매치의 텍스트와 오프셋을 정확히 반환한다', () => {
        const {matches, error, truncated} = findMatches('\\d+', 'g', 'abc123def456')
        expect(error).toBe('')
        expect(truncated).toBe(false)
        expect(matches).toEqual([
            {index: 0, text: '123', start: 3, end: 6, groups: []},
            {index: 1, text: '456', start: 9, end: 12, groups: []},
        ])
    })

    it('g 플래그 없음: 첫 매치만 반환한다 (전체 매치와 구분)', () => {
        const {matches} = findMatches('\\d+', '', 'abc123def456')
        expect(matches).toEqual([{index: 0, text: '123', start: 3, end: 6, groups: []}])
    })

    it('i 플래그 on/off: 대소문자 무시 여부가 결과를 바꾼다', () => {
        const withI = findMatches('abc', 'gi', 'ABC abc').matches
        expect(withI.map((m) => [m.text, m.start])).toEqual([['ABC', 0], ['abc', 4]])

        const withoutI = findMatches('abc', 'g', 'ABC abc').matches
        expect(withoutI.map((m) => [m.text, m.start])).toEqual([['abc', 4]])
    })

    it('m 플래그 on/off: ^가 줄 시작에 매치되는지 여부가 결과를 바꾼다', () => {
        const withM = findMatches('^\\w+', 'gm', 'foo\nbar').matches
        expect(withM.map((m) => m.text)).toEqual(['foo', 'bar'])

        const withoutM = findMatches('^\\w+', 'g', 'foo\nbar').matches
        expect(withoutM.map((m) => m.text)).toEqual(['foo'])
    })

    it('s 플래그 on/off: .이 개행에 매치되는지 여부가 결과를 바꾼다', () => {
        expect(findMatches('a.b', 'gs', 'a\nb').matches.map((m) => m.text)).toEqual(['a\nb'])
        expect(findMatches('a.b', 'g', 'a\nb').matches).toEqual([])
    })

    it('번호 그룹: 각 그룹의 번호와 텍스트를 정확히 추출한다', () => {
        const {matches} = findMatches('(\\d{4})-(\\d{2})-(\\d{2})', 'g', '2026-07-12')
        expect(matches).toHaveLength(1)
        expect(matches[0].text).toBe('2026-07-12')
        expect(matches[0].groups).toEqual([
            {num: 1, name: null, text: '2026'},
            {num: 2, name: null, text: '07'},
            {num: 3, name: null, text: '12'},
        ])
    })

    it('명명 그룹: 이름과 값을 함께 반환한다', () => {
        const {matches} = findMatches('(?<year>\\d{4})-(?<month>\\d{2})', 'g', '2026-07')
        expect(matches[0].groups).toEqual([
            {num: 1, name: 'year', text: '2026'},
            {num: 2, name: 'month', text: '07'},
        ])
    })

    it('참여하지 않은 옵셔널 그룹은 text가 null이다', () => {
        const {matches} = findMatches('(a)(b)?', 'g', 'a')
        expect(matches[0].groups).toEqual([
            {num: 1, name: null, text: 'a'},
            {num: 2, name: null, text: null},
        ])
    })

    it('문법 오류: SyntaxError 메시지를 error로 반환하고 매치는 비운다', () => {
        const {matches, error} = findMatches('(', 'g', 'abc')
        expect(matches).toEqual([])
        expect(error).toContain('Invalid regular expression')
        expect(error).toContain('Unterminated group')
    })

    it('빈 매치가 가능한 패턴도 무한루프 없이 위치별로 반환한다', () => {
        const {matches} = findMatches('a*', 'g', 'bb')
        expect(matches.map((m) => [m.text, m.start])).toEqual([['', 0], ['', 1], ['', 2]])
    })

    it('maxMatches 초과 시 잘라내고 truncated를 표시한다', () => {
        const {matches, truncated} = findMatches('\\d', 'g', '12345', 2)
        expect(matches.map((m) => m.text)).toEqual(['1', '2'])
        expect(truncated).toBe(true)
    })

    it('빈 패턴이면 에러 없이 매치 없음', () => {
        expect(findMatches('', 'g', 'abc')).toEqual({matches: [], error: '', truncated: false})
    })
})

describe('captureGroupNames', () => {
    it('일반/명명/비캡처 그룹을 구분해 순서대로 이름을 뽑는다', () => {
        expect(captureGroupNames('(a)(?<x>b)(?:c)(?=d)(e)')).toEqual([null, 'x', null])
    })

    it('룩비하인드 (?<= (?<! 는 캡처 그룹으로 세지 않는다', () => {
        expect(captureGroupNames('(?<=x)(?<name>\\d)(?<!y)')).toEqual(['name'])
        // 실제 매치에도 이름이 정확히 붙는지
        const {matches} = findMatches('(?<=x)(?<name>\\d)', 'g', 'x5 y7')
        expect(matches).toHaveLength(1)
        expect(matches[0].groups).toEqual([{num: 1, name: 'name', text: '5'}])
    })

    it('문자 클래스/이스케이프 안의 괄호는 무시한다', () => {
        expect(captureGroupNames('[(](a)\\((b)')).toEqual([null, null])
    })
})

describe('replacePreview', () => {
    it('$1 치환: g 플래그면 모든 매치를 치환한다', () => {
        const {result, error, count} = replacePreview('(\\d+)', 'g', 'a1b22', '[$1]')
        expect(error).toBe('')
        expect(result).toBe('a[1]b[22]')
        expect(count).toBe(2)
    })

    it('g 플래그 없음: 첫 매치만 치환한다 (전체 치환과 구분)', () => {
        const {result, count} = replacePreview('(\\d+)', '', 'a1b22', '[$1]')
        expect(result).toBe('a[1]b22')
        expect(count).toBe(1)
    })

    it('명명 그룹 치환 $<name>을 지원한다', () => {
        const {result} = replacePreview('(?<num>\\d+)', 'g', 'a1b22', '<$<num>>')
        expect(result).toBe('a<1>b<22>')
    })

    it('문법 오류 시 error를 반환하고 result는 빈 문자열', () => {
        const {result, error} = replacePreview('[', 'g', 'abc', 'x')
        expect(result).toBe('')
        expect(error).toContain('Invalid regular expression')
    })

    it('치환 결과가 원본과 실제로 달라진다 (매치 없으면 원본 유지)', () => {
        expect(replacePreview('z+', 'g', 'abc', 'X')).toEqual({result: 'abc', error: '', count: 0})
    })
})

describe('segmentText', () => {
    it('매치/비매치 구간을 원문 그대로 순서대로 쪼갠다', () => {
        const {matches} = findMatches('\\d+', 'g', 'abc123def456')
        expect(segmentText('abc123def456', matches)).toEqual([
            {text: 'abc', matchIndex: null},
            {text: '123', matchIndex: 0},
            {text: 'def', matchIndex: null},
            {text: '456', matchIndex: 1},
        ])
    })

    it('텍스트 전체가 매치면 비매치 세그먼트가 없다', () => {
        const {matches} = findMatches('.+', 'g', 'abc')
        expect(segmentText('abc', matches)).toEqual([{text: 'abc', matchIndex: 0}])
    })

    it('빈 매치(길이 0)는 세그먼트를 만들지 않고 원문을 보존한다', () => {
        const {matches} = findMatches('a*', 'g', 'bb')
        const segs = segmentText('bb', matches)
        expect(segs.map((s) => s.text).join('')).toBe('bb')
        expect(segs.every((s) => s.matchIndex === null)).toBe(true)
    })
})

describe('REGEX_PRESETS', () => {
    it('프리셋 6개: 이메일/URL/IP/날짜/전화번호/UUID', () => {
        expect(REGEX_PRESETS.map((p) => p.id)).toEqual([
            'email', 'url', 'ipv4', 'date', 'phone-kr', 'uuid',
        ])
    })

    it('각 프리셋은 자기 샘플 텍스트에서 기대한 매치를 정확히 찾는다', () => {
        const expected: Record<string, string[]> = {
            email: ['alice@example.com', 'bob.lee@test.co.kr'],
            url: ['https://example.com/docs?page=1', 'http://localhost:8080/api'],
            ipv4: ['192.168.0.10', '10.0.0.1'],
            date: ['2026-07-12', '2026-12-31'],
            'phone-kr': ['010-1234-5678', '01198765432'],
            uuid: ['550e8400-E29B-41d4-a716-446655440000'],
        }
        for (const p of REGEX_PRESETS) {
            const {matches, error} = findMatches(p.pattern, p.flags, p.sample)
            expect(error, p.id).toBe('')
            expect(matches.map((m) => m.text), p.id).toEqual(expected[p.id])
        }
    })

    it('UUID 프리셋은 i 플래그로 대소문자 혼용을 잡는다 (i 없으면 못 잡음)', () => {
        const p = REGEX_PRESETS.find((x) => x.id === 'uuid')!
        expect(findMatches(p.pattern, 'g', p.sample).matches).toEqual([])
    })

    it('날짜 프리셋은 연/월/일 그룹을 캡처한다', () => {
        const p = REGEX_PRESETS.find((x) => x.id === 'date')!
        const {matches} = findMatches(p.pattern, p.flags, p.sample)
        expect(matches[0].groups.map((g) => g.text)).toEqual(['2026', '07', '12'])
    })
})
