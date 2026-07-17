<template>
  <div class="mx-auto max-w-2xl px-6 py-8">
    <h1 class="mb-1 text-xl font-semibold text-foreground">개인정보처리방침</h1>
    <p class="mb-6 text-xs text-muted-foreground">시행일: 2026-07-17</p>

    <section class="mb-6 rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-sm font-medium text-foreground">수집 항목</h2>
      <ul class="flex flex-col gap-2 text-sm text-muted-foreground">
        <li>
          <span class="text-foreground">소셜 로그인 정보</span>
          (제공자, 이메일(수집 시), 닉네임) — 소셜 로그인 기능은 아직 제공되지 않으며,
          도입 <span class="font-medium text-foreground">예정</span>인 항목입니다. 기능이 열리는 시점에 본 방침을 개정합니다.
        </li>
        <li>
          <span class="text-foreground">작업 이력 메타데이터</span>
          — 도구 사용 기록(어떤 도구를 언제 사용했는지 등)
        </li>
        <li>
          <span class="text-foreground">GA4 쿠키·행태정보</span>
          — 서비스 이용 분석을 위한 Google Analytics 4 쿠키 및 행태정보
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
          — 회원 탈퇴 기능은 아직 제공되지 않으며, 도입되면 탈퇴 즉시 개인을 식별할 수 있는 정보를 파기합니다.
        </li>
        <li>
          <span class="text-foreground">업로드 파일</span>
          — 처리에 사용한 입력 파일은 작업 완료 직후 즉시 삭제되고, 결과 파일은 설정된 TTL(보존 기간)이 지나면 자동 삭제됩니다.
        </li>
      </ul>
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
            <td class="py-2 pr-4 font-mono tabular-nums text-foreground">2026-07-17</td>
            <td class="py-2">최초 시행</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
