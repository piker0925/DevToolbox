# ADR-0030 도구 단일 구역 원칙 — 복수 구역 노출 폐기

## 상태

확정 (2026-07-21) · 097(single-zone-per-tool) 구현 완료. ADR-0023 "구역은 도구의 소속이 아니라 탐색 컨텍스트다 — 한 도구가 복수 구역에 노출될 수 있다" 원칙 중 이 문장만 대체한다(supersedes: ADR-0023, 해당 문장에 한정).

## 배경

ADR-0023은 `/tools/:id` 상세 URL을 구역과 분리하면서, "구역은 탐색 컨텍스트일 뿐 도구의 소속이 아니다"라는 근거로 한 도구가 여러 구역의 사이드바·구역 홈에 동시에 노출되는 것을 허용했다(예: EXIF 제거기가 `/dev`·`/files` 양쪽). 실제로 `front/src/api/mock.ts`에서 8개 도구가 이 방식으로 2개 구역에 걸쳐 있었다(image-diff, colorblind-simulator, image-to-ascii, timezone-converter, random-palette, wordcloud, pet-age-converter, 그리고 원래 단일이었던 text-diff는 이번에 dev→life로 재분류).

사용자 검토 결과, 한 도구가 두 구역에 동시 노출되는 것은 원치 않는 동작으로 확인됐다. 이유는 두 가지다:

1. **탐색 단순성**: 사용자가 `/fun`에서 본 도구를 나중에 `/dev`에서도 마주치면 "같은 도구가 왜 두 군데 있지"라는 혼란을 준다. 구역은 서로 다른 서브 프로덕트 느낌을 내야 하는데(ADR-0027), 도구가 겹치면 그 경계가 흐려진다.
2. **정체성 일관성**: 포트폴리오 관점에서 도구 하나는 "이 도구가 누구를 위한 것인가"에 대한 한 가지 답을 가져야 한다. 복수 구역 노출은 이 답을 흐릿하게 만든다.

## 결정

**도구는 정확히 하나의 구역에만 노출된다. `zones` 배열은 항상 원소 1개다.**

- `front/src/api/mock.ts`의 각 도구 메타에서 `zones` 배열을 원소 1개로 축소한다. 이번 작업에서 8개 도구를 아래와 같이 재배정했다:

  | 도구 id | 기존 zones | 확정 구역 |
  |---|---|---|
  | image-diff | files, dev | files |
  | colorblind-simulator | files, dev | files |
  | image-to-ascii | files, fun | files |
  | timezone-converter | dev, life | life |
  | random-palette | dev, fun | fun |
  | wordcloud | fun, dev | fun |
  | pet-age-converter | life, fun | life |
  | text-diff | dev (원래 단일) | life (재분류) |

- `/tools/:id` 상세 URL이 구역과 분리된 원칙(ADR-0023의 나머지 부분)은 유지한다 — 이 ADR은 오직 "노출 구역이 복수일 수 있다"는 문장만 뒤집는다. 도구 상세 페이지, 북마크, `moduleId` 기반 좋아요·댓글은 무영향.
- 구역별 도구 목록·개수(`ZoneHomePage`, 랜딩 페이지 구역 카드, ⌘K `CommandPalette`)는 모두 `zones` 배열에서 동적으로 파생되므로, 로직 변경 없이 데이터(`mock.ts`)만 바꾸면 자동 반영된다.
- 카테고리(`category`, 예: '이미지', '텍스트')와 구역(`zones`)은 별개 축이다 — 같은 카테고리의 도구들이 서로 다른 구역에 속할 수 있다(예: '텍스트' 카테고리의 text-diff는 life, regex-tester는 dev). 카테고리는 구역 소속을 강제하지 않는다.

## 기각한 대안

- **현행 유지(복수 구역 허용)** — ADR-0023 원안. 사용자가 명시적으로 원치 않는다고 확인했으므로 기각.
- **구역별 "즐겨 찾는 대체 도구" 카드로 우회 노출** — 실제 노출과 유사한 혼란을 재현할 뿐이므로 채택하지 않음. 도구는 한 구역에만 있고, 다른 구역에서 찾고 싶으면 ⌘K 전역 검색을 쓴다.

## 결과

- `front/src/api/mock.ts` 전체에서 `zones` 배열 길이가 1을 초과하는 항목 0개(회귀 방지는 `front/src/api/mock.test.ts`의 카테고리별 구역 포함 검증에서 다룬다 — '텍스트' 카테고리는 이번 재분류로 dev 전제 검증 대상에서 제외됨).
- `front/src/pages/LandingPage.test.ts`의 `/files` 구역 도구 수 하드코딩 테스트는 무변경(image-diff·colorblind-simulator·image-to-ascii가 이미 files를 포함하고 있었으므로 files 구역 카운트는 그대로).
- CONTEXT.md "도메인 용어 > 구역" 항목의 "한 도구가 복수 구역에 노출될 수 있다" 서술을 "도구는 단일 구역에만 노출된다(ADR-0030)"로 갱신 필요.
