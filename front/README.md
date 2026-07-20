# OnTool — 프론트엔드

OnTool 종합 도구 포털의 프론트엔드. Vue 3(Composition API) + Vite + TypeScript + Tailwind CSS v4 + shadcn/vue.

프로젝트 전체 개요·아키텍처·실행 방법은 [루트 README](../README.md)를 참조한다.

## 개발

```bash
pnpm install
pnpm dev        # 개발 서버 (http://localhost:5173)
pnpm test       # 단위 테스트 (vitest)
pnpm run build  # 타입체크(vue-tsc) + 프로덕션 빌드
```

> `pnpm test`(vitest)는 트랜스파일만 하고 타입체크는 하지 않는다. 커밋 전 `pnpm run build`(`vue-tsc -b && vite build`)까지 통과해야 완료로 본다. `.githooks/pre-push`가 push 시 이를 강제한다(최초 1회 `git config core.hooksPath .githooks` 필요).

## 구조

- `src/pages/` — 랜딩 대문·구역 홈·도구 상세·마이페이지
- `src/components/` — 도구별 커스텀 페이지·공용 컴포넌트(FileUploader·결과 패널 등)
- `src/components/tools/` — 프론트 로컬 계산 도구
- `src/config/` — 도구 레지스트리(`frontendToolRegistry.ts`)·게임 셸(`shellComponents.ts`)
- `src/api/` — API 클라이언트·모듈 카탈로그(`mock.ts`)
- `src/composables/` · `src/utils/` — 재사용 로직·순수 계산 유틸
