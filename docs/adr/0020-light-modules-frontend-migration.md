# ADR-0020 순수-계산 Light 모듈의 프론트엔드 이전

## 상태

확정 (2026-07-15)

## 배경

Light 모듈 중 서버 자원이 실제로 필요 없고(외부 API·CORS·무거운 네이티브 라이브러리 불요) 브라우저에서 동등 품질로 재현 가능한 것들이 있다. 이들을 프론트 로컬 처리로 옮기면:

- **크립토 보안**: AES/HMAC의 키·평문이 서버로 전송되지 않는다(브라우저를 떠나지 않음).
- **즉시성**: 네트워크 왕복(수십~수백 ms + 큐) 제거.
- **요청 소멸**: 서버 부하·요청 자체가 사라진다(2 OCPU 단일 VM).

효율 이득 자체는 작다(Light는 웹 스레드에서 ms 처리). 주 병목인 Heavy(PDF·이미지·영상)는 이전 대상이 아니며 그건 스케줄러(ADR-0019)·Rate Limiting(037/040 후보)이 담당한다. 이미 `text-diff`·`regex-tester`·`totp`·`jwt`가 프론트-전용으로 전환된 선례가 있다.

## 결정

**이전 필터를 통과하는 Light 모듈을 프론트 로컬 처리로 이전하고, 해당 백엔드 모듈을 제거한다.**

이전(백엔드 제거):
- `subnet-calc` → `front/src/utils/subnetCalc.ts` (순수 IP 산술)
- `url-parser` → `front/src/utils/urlParser.ts` (브라우저 URL API)
- `cron` → `front/src/utils/cronExpr.ts` (cronstrue 설명 + cron-parser 다음 실행)
- `hmac` → `front/src/utils/hmac.ts` (Web Crypto + spark-md5)
- `aes` → `front/src/utils/aes.ts` (Web Crypto)
- `case-converter` → `front/src/utils/caseConvert.ts` (순수 문자열 변환, `text-utils` 통합 페이지 탭에서 백엔드 호출을 로컬로 교체)
- `html-entity` → `front/src/utils/htmlEntity.ts` (`he` 라이브러리로 full named-entity, `encoder` 통합 페이지 탭)
- 잔여 死코드였던 `text-diff`·`regex-tester`·`totp` 백엔드 모듈도 함께 제거(이미 프론트-전용이었음).

`case-converter`·`html-entity`는 "형제 기능은 로컬인데 이 탭만 백엔드를 왕복"하던 불일치를 없앤 경우 — 효율 이득은 작지만 비용·리스크가 낮고 일관성을 회복한다.

이전 안 함(백엔드 유지):
- `multi-hash` — Web Crypto가 MD5·SHA3·BLAKE2b 미지원(부분만 가능).
- `html-fetch` — CORS, 서버 프록시 필수.
- `vuln-scan` — OSV.dev API 호출 + 의존성 파싱.
- `sql-formatter`·`xml-formatter` — 백엔드 JSQLParser·JAXP가 실제 파서 기반의 더 나은 구현이고, 보안 이득이 없으며 서버 절감이 미미해 이전 이득이 얇다(원래 계획의 "Tier 3"을 취소하고 백엔드 유지).
- 모든 Heavy 모듈.

## 크립토 파리티 결정

프론트 Web Crypto가 백엔드 알고리즘/모드를 전부 재현하지 못하므로 모듈별로 명시 결정한다:

- **HMAC-MD5**: Web Crypto는 MD5를 지원하지 않는다 → `spark-md5`로 RFC 2104 HMAC을 직접 구성해 **파리티 유지**. (RFC 2202 벡터로 검증)
- **AES-ECB**: Web Crypto는 ECB를 의도적으로 미지원하고 ECB는 패턴 노출로 비권장이다 → **드롭**(CBC/GCM/CTR만 제공). 파리티 손실을 수용하며, 이는 사실상 보안 개선이다.

## 백엔드 제거가 안전한 근거

"백엔드 모듈을 지우면 좋아요·댓글의 `moduleId` 참조가 깨지지 않는가?"에 대한 검증:

- `ToolStatsController`/`CommentController`는 `{moduleId}`를 **모듈 레지스트리로 검증하지 않는다**. 임의 문자열을 그대로 받아 upsert/조회한다.
- 레지스트리 검증(`MODULE_NOT_FOUND`)은 `/run`·`/upload`(`ToolService.findModule`)에만 있다.
- 이전한 도구의 id는 프론트-전용 도구로 계속 존재하므로, 백엔드 모듈을 제거해도 좋아요·사용수·댓글은 그대로 유효하다.

따라서 "유지+숨김"(과거 `text-diff` 등의 방식)은 이 프로젝트(프론트가 유일한 클라이언트)에서는 死코드만 남기며, **제거가 정합적**이다.

## 의존성 변화

- 제거(백엔드): `com.eatthepath:java-otp`(TOTP), `com.cronutils:cron-utils`(Cron), `io.github.java-diff-utils`(Diff), `org.apache.commons:commons-text`(HtmlEntity).
- 추가(프론트): `cronstrue`, `cron-parser`, `spark-md5`, `he`(HTML 엔티티).

## 결과

- 백엔드에는 서버가 진짜 필요한 것(Heavy 파일 처리, 외부 API, 큐/스케줄러/SSE/admission control)만 남는다.
- `HIDDEN_MODULE_IDS`에서 이전 도구를 제거(백엔드가 반환하지 않으므로 숨길 필요 없음). 통합 도구로 흡수된 모듈(인코더/변환/QR 등)만 잔류.
- 검증: 프론트 유닛 테스트(RFC 벡터·Node crypto 독립 오라클·암호문≠평문 라운드트립), 백엔드 전체 테스트 통과, 프로덕션 빌드 성공, 실브라우저 스모크(cron·aes·subnet).
