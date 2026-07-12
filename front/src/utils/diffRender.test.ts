import {describe, expect, it} from 'vitest'
import {buildDiffModel, splitDocLines} from './diffRender'

describe('splitDocLines', () => {
  it('빈 문서는 0라인', () => {
    expect(splitDocLines('')).toEqual([])
  })

  it('마지막 개행 뒤 빈 문자열은 제거하고 중간 빈 라인은 유지한다', () => {
    expect(splitDocLines('a\n\nb\n')).toEqual(['a', '', 'b'])
    expect(splitDocLines('a\nb')).toEqual(['a', 'b'])
  })
})

describe('buildDiffModel — 동일 입력', () => {
  it('identical=true, 통계 0, 전 라인 context', () => {
    const m = buildDiffModel('foo\nbar', 'foo\nbar')
    expect(m.identical).toBe(true)
    expect(m.stats).toEqual({addedLines: 0, removedLines: 0, blocks: 0, similarity: 100})
    expect(m.rows).toHaveLength(2)
    expect(m.rows[0].left).toEqual({type: 'context', lineNum: 1, text: 'foo', spans: null})
    expect(m.rows[0].right).toEqual({type: 'context', lineNum: 1, text: 'foo', spans: null})
    expect(m.rows[0].blockIndex).toBeNull()
    expect(m.inline[1]).toEqual({
      type: 'context',
      oldLineNum: 2,
      newLineNum: 2,
      text: 'bar',
      spans: null,
      blockIndex: null,
    })
  })
})

describe('buildDiffModel — 추가만 있는 경우', () => {
  it('추가 라인이 오른쪽에만 나타나고 왼쪽은 empty 필러', () => {
    const m = buildDiffModel('a\nb', 'a\nb\nc\nd')
    expect(m.identical).toBe(false)
    expect(m.stats).toEqual({addedLines: 2, removedLines: 0, blocks: 1, similarity: 50})

    // context 2행 + 변경 2행
    expect(m.rows).toHaveLength(4)
    expect(m.rows[2].left).toEqual({type: 'empty', lineNum: null, text: '', spans: null})
    expect(m.rows[2].right).toEqual({type: 'add', lineNum: 3, text: 'c', spans: null})
    expect(m.rows[2].blockIndex).toBe(0)
    expect(m.rows[3].right).toEqual({type: 'add', lineNum: 4, text: 'd', spans: null})

    // inline: context a, context b, add c, add d
    expect(m.inline.map((l) => [l.type, l.text])).toEqual([
      ['context', 'a'],
      ['context', 'b'],
      ['add', 'c'],
      ['add', 'd'],
    ])
    expect(m.inline[2]).toEqual({type: 'add', oldLineNum: null, newLineNum: 3, text: 'c', spans: null, blockIndex: 0})
  })
})

describe('buildDiffModel — 삭제만 있는 경우', () => {
  it('삭제 라인이 왼쪽에만 나타나고 오른쪽은 empty 필러, 유지 라인은 영향 없음', () => {
    const m = buildDiffModel('keep\ngone', 'keep')
    expect(m.stats).toEqual({addedLines: 0, removedLines: 1, blocks: 1, similarity: 50})
    expect(m.rows).toHaveLength(2)
    // 조건에 안 맞는(유지되는) 라인은 context로 살아남는다
    expect(m.rows[0].left).toEqual({type: 'context', lineNum: 1, text: 'keep', spans: null})
    expect(m.rows[1].left).toEqual({type: 'remove', lineNum: 2, text: 'gone', spans: null})
    expect(m.rows[1].right).toEqual({type: 'empty', lineNum: null, text: '', spans: null})
    expect(m.inline[1]).toEqual({
      type: 'remove',
      oldLineNum: 2,
      newLineNum: null,
      text: 'gone',
      spans: null,
      blockIndex: 0,
    })
  })
})

describe('buildDiffModel — 변경 라인 쌍의 단어 단위 하이라이트', () => {
  it('쌍이 되는 remove/add 라인은 단어 diff spans를 가진다', () => {
    const m = buildDiffModel('hello cruel world', 'hello kind world')
    expect(m.stats).toEqual({addedLines: 1, removedLines: 1, blocks: 1, similarity: 0})
    expect(m.rows).toHaveLength(1)

    const row = m.rows[0]
    expect(row.left.type).toBe('remove')
    expect(row.left.lineNum).toBe(1)
    expect(row.right.type).toBe('add')
    expect(row.right.lineNum).toBe(1)

    // 변경된 단어만 changed=true, 나머지는 changed=false — 조각 텍스트를 이으면 원문
    expect(row.left.spans).toEqual([
      {text: 'hello ', changed: false},
      {text: 'cruel', changed: true},
      {text: ' world', changed: false},
    ])
    expect(row.right.spans).toEqual([
      {text: 'hello ', changed: false},
      {text: 'kind', changed: true},
      {text: ' world', changed: false},
    ])
    expect(row.left.spans!.map((s) => s.text).join('')).toBe('hello cruel world')
    expect(row.right.spans!.map((s) => s.text).join('')).toBe('hello kind world')

    // inline: remove 먼저, add 나중 — 같은 spans 공유
    expect(m.inline.map((l) => l.type)).toEqual(['remove', 'add'])
    expect(m.inline[0].spans).toEqual(row.left.spans)
    expect(m.inline[1].spans).toEqual(row.right.spans)
  })

  it('쌍 개수를 넘는 라인(추가가 더 많은 경우)은 spans 없이 렌더된다', () => {
    const m = buildDiffModel('old line', 'new line\nextra line')
    expect(m.stats.addedLines).toBe(2)
    expect(m.stats.removedLines).toBe(1)
    expect(m.rows).toHaveLength(2)
    // 1행: 쌍 → 양쪽 spans 존재
    expect(m.rows[0].left.spans).not.toBeNull()
    expect(m.rows[0].right.spans).not.toBeNull()
    // 2행: 왼쪽 필러, 오른쪽 spans 없음
    expect(m.rows[1].left.type).toBe('empty')
    expect(m.rows[1].right).toEqual({type: 'add', lineNum: 2, text: 'extra line', spans: null})
  })
})

describe('buildDiffModel — wordDiff 옵션 on/off (성능 가드)', () => {
  const original = 'hello cruel world'
  const revised = 'hello kind world'

  it('wordDiff=true(기본)면 spans가 채워진다', () => {
    const on = buildDiffModel(original, revised)
    expect(on.rows[0].left.spans).not.toBeNull()
  })

  it('wordDiff=false면 spans가 null이고 라인 텍스트는 유지된다', () => {
    const off = buildDiffModel(original, revised, {wordDiff: false})
    expect(off.rows[0].left.spans).toBeNull()
    expect(off.rows[0].right.spans).toBeNull()
    expect(off.rows[0].left.text).toBe('hello cruel world')
    expect(off.rows[0].right.text).toBe('hello kind world')
    // 통계는 wordDiff 여부와 무관하게 동일
    expect(off.stats).toEqual(buildDiffModel(original, revised).stats)
  })

  it('maxWordDiffLineLength를 넘는 라인은 단어 diff를 생략한다', () => {
    const longOld = 'x'.repeat(20) + ' old'
    const longNew = 'x'.repeat(20) + ' new'
    const m = buildDiffModel(longOld, longNew, {maxWordDiffLineLength: 10})
    expect(m.rows[0].left.spans).toBeNull()
    expect(m.rows[0].right.spans).toBeNull()
  })
})

describe('buildDiffModel — 변경 블록 인덱스와 라인 번호 연속성', () => {
  it('떨어진 변경은 별도 블록으로 세고 blockIndex가 순서대로 부여된다', () => {
    const original = 'a\nb\nc\nd\ne'
    const revised = 'a\nB\nc\nd\nE'
    const m = buildDiffModel(original, revised)
    expect(m.stats).toEqual({addedLines: 2, removedLines: 2, blocks: 2, similarity: 60})

    // 행 구조: context(a), 변경(b→B), context(c), context(d), 변경(e→E)
    expect(m.rows.map((r) => r.blockIndex)).toEqual([null, 0, null, null, 1])
    expect(m.rows.map((r) => [r.left.lineNum, r.right.lineNum])).toEqual([
      [1, 1],
      [2, 2],
      [3, 3],
      [4, 4],
      [5, 5],
    ])

    // inline 순서와 번호: context/remove/add 교차 확인
    expect(m.inline.map((l) => [l.type, l.oldLineNum, l.newLineNum, l.blockIndex])).toEqual([
      ['context', 1, 1, null],
      ['remove', 2, null, 0],
      ['add', null, 2, 0],
      ['context', 3, 3, null],
      ['context', 4, 4, null],
      ['remove', 5, null, 1],
      ['add', null, 5, 1],
    ])
  })
})

describe('buildDiffModel — 빈 입력 경계', () => {
  it('둘 다 빈 문자열이면 identical, 행 없음', () => {
    const m = buildDiffModel('', '')
    expect(m.identical).toBe(true)
    expect(m.rows).toEqual([])
    expect(m.inline).toEqual([])
    expect(m.stats.similarity).toBe(100)
  })

  it('원본만 빈 경우 전부 add', () => {
    const m = buildDiffModel('', 'a\nb')
    expect(m.stats).toEqual({addedLines: 2, removedLines: 0, blocks: 1, similarity: 0})
    expect(m.rows.map((r) => [r.left.type, r.right.type, r.right.text])).toEqual([
      ['empty', 'add', 'a'],
      ['empty', 'add', 'b'],
    ])
  })
})
