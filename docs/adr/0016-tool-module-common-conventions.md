# ADR-0016 도구 모듈 공통 컨벤션 — ToolParams·ToolResult.ofJson·ToolStats 원자 갱신

## 상태
확정

## 배경

25개 이상의 `ToolModule` 구현체에 도구별 세부 기능(옵션 파라미터, 검증, 구조화 출력)을 대거 추가하는 작업을 진행하면서 세 가지 기존 결함이 반복적으로 문제를 일으켰다.

1. **파라미터 파싱 중복**: 각 모듈이 `input.params().getOrDefault(key, default)`를 직접 호출하고, 정수 범위 검증·enum 검증을 모듈마다 다르게(또는 하지 않고) 구현했다. 검증 실패 메시지도 모듈마다 형식이 달랐다.
2. **결과 표현의 한계**: `ToolResult`는 파일 또는 순수 텍스트만 표현했다. 다중 해시·URL 파서·취약점 스캔처럼 표 형태나 키-값 목록이 자연스러운 도구도 어쩔 수 없이 "algo: hash\n" 같은 개행 텍스트로 반환해 프론트가 개별 항목 복사 등을 구현할 수 없었다.
3. **좋아요 카운터의 lost update**: `ToolStatsService.incrementLikeCount`가 `findById → setLikeCount(get()+1) → save`의 read-modify-write 패턴이라 동시 요청 시 증가분이 유실될 수 있었고, 좋아요 취소(감소) 기능 자체가 없어 버튼을 누를수록 무한히 카운트가 올라가는 버그가 있었다.

## 검토한 선택지

**파라미터 파싱**
| 방식 | 문제점 |
|------|--------|
| A. 모듈마다 현행 유지 | 검증 메시지 불일치, 신규 파라미터마다 보일러플레이트 반복 |
| B. 모든 기존 모듈을 일괄 리팩터링 | 이번 라운드에서 손대지 않는 모듈까지 회귀 위험을 키움 |
| C. 공통 헬퍼 신설 + 신규/수정 모듈부터 점진 채택 | — |

**구조화 출력**
| 방식 | 문제점 |
|------|--------|
| A. 프론트가 텍스트를 파싱해서 표로 재구성 | 도구마다 다른 텍스트 포맷을 프론트가 억지로 파싱해야 함, 백엔드 변경 시 프론트가 같이 깨짐 |
| B. `ToolResult`에 구조화 타입 추가 | — |

## 결정

**C + B: `ToolParams` 헬퍼 신설(점진 채택) + `ToolResult.ofJson` 구조화 출력 컨벤션 + `ToolStats` 원자 UPDATE**

1. **`ToolParams`** (`back/src/main/java/com/back/tool/model/ToolParams.java`): `getString/requireString/getInt(key,default,min,max)/getBool/getEnum`. 검증 실패 시 일관된 한국어 `ToolProcessingException` 메시지("파라미터 'size'는 1~2000 사이여야 합니다. (입력값: 5000)"). 기존 모듈은 그대로 두고, 이번 라운드에서 새로 파라미터를 추가하거나 수정하는 모듈부터 적용한다.
2. **`ToolResult.ofJson(payload)`**: Jackson으로 직렬화한 JSON을 `textResult`에 담는다. 컨벤션으로 최상위 `type` 필드를 두고 `{"type":"table","columns":[...],"rows":[[...]]}` 또는 `{"type":"keyvalue","items":[{"key":..,"value":..}]}` 형태를 표준으로 삼는다. 프론트 `parseStructuredResult` + `StructuredResultView.vue`가 이 컨벤션을 공용으로 렌더링하고 행별 복사를 제공한다.
3. **`ToolStats` 원자 갱신**: `ToolStatsRepository`에 `@Modifying @Query`로 `INSERT ... ON DUPLICATE KEY UPDATE`(증가) 및 `UPDATE ... WHERE like_count > 0`(감소, 0 미만 방지)를 추가해 read-modify-write를 제거했다. `DELETE /api/v1/tools/{moduleId}/like` 엔드포인트를 신설해 좋아요 취소를 지원하고, 프론트는 `useLikes`(localStorage)로 브라우저별 좋아요 상태를 추적해 토글 UI를 구현한다.

## 이유

- 기존 모듈 일괄 전환(안 B)은 이번 라운드의 핵심 목표(도구별 기능 심화)와 무관한 회귀 위험을 키우므로 기각. 점진 채택으로 새 코드는 컨벤션을 따르되 기존 코드는 건드리지 않는다.
- 구조화 출력은 새 컴포넌트(`StructuredResultView`) 하나로 여러 도구(다중 해시, URL 파서, 서브넷 계산기, RSA 키, 취약점 스캔)가 표/키값 렌더링과 행별 복사를 공짜로 얻는다 — 도구마다 커스텀 결과 UI를 만드는 것보다 유지보수 비용이 낮다.
- `ON DUPLICATE KEY UPDATE` 단일 문장은 "행 존재 확인 후 UPDATE"보다 원자적이며, MySQL에서 별도 트랜잭션 격리 수준 조정 없이 갭 락 데드락 위험도 없다.

## 결과

- 이번 라운드에서 신규/수정된 모듈은 대부분 `ToolParams`/`ToolResult.ofJson`을 채택했지만, 전체 모듈에 일관되게 적용되지는 않았다 — 리뷰에서 `MarkdownToPdfModule`/`ImageToPdfModule`/`PdfSplitModule`/`ImageFormatModule`/`ImageResizeModule`의 enum형 파라미터가 여전히 수동 파싱임을 확인했다(백로그로 이관, `.scratch/backlog-tool-ux.md` 참조).
- 좋아요 동시성 정합성은 20개 스레드 동시 증가 테스트(`ToolStatsServiceTest.incrementUseCount_concurrentRequests_noLostUpdate`)로 검증했다.
