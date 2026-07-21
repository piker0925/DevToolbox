<template>
  <div class="mx-auto max-w-2xl px-6 py-8">
    <h1 class="mb-1 text-xl font-semibold text-foreground">개인정보처리방침</h1>
    <p class="mb-6 text-xs text-muted-foreground">시행일: 2026-07-21</p>

    <section class="mb-6 rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-sm font-medium text-foreground">수집 항목</h2>
      <ul class="flex flex-col gap-2 text-sm text-muted-foreground">
        <li>
          <span class="text-foreground">소셜 로그인 정보</span>
          (제공자, 이메일(수집 시), 닉네임) — 구글·카카오 소셜 로그인으로 회원가입·로그인할 때 수집합니다.
          비밀번호는 별도로 저장하지 않습니다.
        </li>
        <li>
          <span class="text-foreground">작업 이력 메타데이터</span>
          — 도구 사용 기록(어떤 도구를 언제 사용했는지 등)
        </li>
        <li>
          <span class="text-foreground">GA4 쿠키·행태정보</span>
          — 서비스 이용 분석을 위한 Google Analytics 4 쿠키 및 행태정보. 기본적으로 수집되며,
          아래 "쿠키 설정"에서 언제든지 거부할 수 있습니다.
        </li>
        <li>
          <span class="text-foreground">서버 로그</span>
          — 접속 IP 등 서버 운영을 위해 자동 수집되는 로그
        </li>
      </ul>
    </section>

    <section class="mb-6 rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-sm font-medium text-foreground">보유 기간</h2>
      <ul class="flex flex-col gap-2 text-sm text-muted-foreground">
        <li>
          <span class="text-foreground">회원 탈퇴 시 파기</span>
          — 마이페이지에서 탈퇴하면 즉시 계정 정보와 인증 토큰을 삭제하고, 즐겨찾기·좋아요 등 개인화 데이터를 파기하며,
          작성한 댓글은 익명화하고 연동된 소셜 계정은 연결을 해제합니다.
        </li>
        <li>
          <span class="text-foreground">업로드 파일</span>
          — 처리에 사용한 입력 파일은 작업 완료 직후 즉시 삭제되고, 결과 파일은 설정된 TTL(보존 기간)이 지나면 자동 삭제됩니다.
        </li>
      </ul>
    </section>

    <section class="mb-6 rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-sm font-medium text-foreground">쿠키 설정</h2>
      <div class="flex items-center justify-between gap-3">
        <p class="text-sm text-muted-foreground">
          Google Analytics 쿠키 수집 동의 상태: <span class="font-medium text-foreground">{{ consentLabel }}</span>
        </p>
        <button
            data-testid="consent-toggle"
            class="shrink-0 rounded-lg border border-border px-3 py-1.5 text-[13px] font-medium text-foreground transition-colors hover:bg-accent"
            @click="toggleConsent"
        >{{ isAllowed ? 'Google Analytics 허용 취소' : 'Google Analytics 허용하기' }}</button>
      </div>
    </section>

    <section class="rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-sm font-medium text-foreground">개정 이력</h2>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm text-muted-foreground">
          <thead>
          <tr class="border-b border-border text-xs text-muted-foreground">
            <th class="py-2 pr-4 font-medium">시행일</th>
            <th class="py-2 font-medium">내용</th>
          </tr>
          </thead>
          <tbody>
          <tr>
            <td class="py-2 pr-4 font-mono tabular-nums text-foreground">2026-07-21</td>
            <td class="py-2">소셜 로그인·회원 탈퇴가 이미 제공 중인 기능임을 반영해 서술 정정, GA4 쿠키 동의 절차 추가</td>
          </tr>
          <tr>
            <td class="py-2 pr-4 font-mono tabular-nums text-foreground">2026-07-17</td>
            <td class="py-2">최초 시행</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {getConsent, setConsent} from '../config/consent'
import {disableAnalytics, initAnalytics} from '../config/analytics'

const consent = ref(getConsent())
const isAllowed = computed(() => consent.value !== 'denied')
const consentLabel = computed(() => {
  if (consent.value === 'granted') return '동의함'
  if (consent.value === 'denied') return '거부함'
  return '기본 허용 중 (아직 직접 선택하지 않음)'
})

function toggleConsent() {
  if (isAllowed.value) {
    setConsent('denied')
    disableAnalytics()
  } else {
    setConsent('granted')
    initAnalytics()
  }
  consent.value = getConsent()
}
</script>
