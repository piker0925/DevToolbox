<template>
  <div
      class="grid min-h-[420px] grid-cols-1 divide-y divide-border overflow-hidden rounded-xl border border-border bg-card lg:grid-cols-2 lg:divide-x lg:divide-y-0"
  >
    <!-- Left: 모드 + 파라미터 + 업로드 -->
    <div class="flex flex-col overflow-hidden">
      <div class="flex h-10 shrink-0 items-center border-b border-border px-4">
        <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">모드</span>
      </div>

      <div class="flex flex-col gap-1.5 border-b border-border p-4">
        <select
            v-model="mode"
            class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
        >
          <option v-for="m in MODES" :key="m.id" :value="m.id">{{ m.label }}</option>
        </select>
      </div>

      <div class="flex flex-col gap-4 border-b border-border p-4">
        <div v-if="mode === 'watermark'" class="flex flex-col gap-1.5">
          <label class="text-[11px] text-muted-foreground">텍스트 워터마크 (비우고 워터마크 이미지만 사용 가능)</label>
          <input
              v-model="watermarkText"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="예: CONFIDENTIAL"
              type="text"
          />
          <label class="mt-2 text-[11px] text-muted-foreground">위치</label>
          <select
              v-model="watermarkPosition"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
          >
            <option v-for="p in WATERMARK_POSITIONS" :key="p" :value="p">{{ p }}</option>
          </select>
          <label class="mt-2 text-[11px] text-muted-foreground">투명도 (0~100)</label>
          <input
              v-model="watermarkOpacity"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="30"
              type="text"
          />
          <p class="mt-2 text-[11px] text-muted-foreground/70">
            — 이미지 워터마크를 함께 쓰려면 파일을 <strong>대상 → 워터마크 이미지</strong> 순서로 업로드하세요
            (위/아래 화살표로 순서 조정 가능).
          </p>
        </div>

        <div v-else-if="mode === 'password'" class="flex flex-col gap-1.5">
          <label class="text-[11px] text-muted-foreground">동작</label>
          <select
              v-model="passwordMode"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
          >
            <option value="SET">비밀번호 설정</option>
            <option value="REMOVE">비밀번호 제거</option>
          </select>
          <label class="mt-2 text-[11px] text-muted-foreground">비밀번호</label>
          <input
              v-model="password"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="비밀번호 입력"
              type="text"
          />
        </div>

        <div v-else class="flex flex-col gap-1.5">
          <label class="text-[11px] text-muted-foreground">헤더 텍스트</label>
          <input
              v-model="headerText"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="예: 회사명"
              type="text"
          />
          <label class="mt-2 text-[11px] text-muted-foreground">푸터 텍스트</label>
          <input
              v-model="footerText"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="예: 대외비"
              type="text"
          />
          <label class="mt-2 text-[11px] text-muted-foreground">페이지 번호 형식 ({page}, {total} 치환)</label>
          <input
              v-model="pageNumberFormat"
              class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
              placeholder="예: {page} / {total}"
              type="text"
          />
          <p class="mt-2 text-[11px] text-muted-foreground/70">— 헤더·푸터·페이지 번호 중 최소 하나는 입력해야 합니다.</p>
        </div>
      </div>

      <div class="flex flex-1 flex-col overflow-auto p-6">
        <FileUploader
            :key="mode"
            :accept="currentMode.fileAccept"
            :moduleId="currentMode.moduleId"
            :multiple="currentMode.fileMultiple"
            :params="currentParams"
            :reorderable="currentMode.reorderable"
            @error="onUploadError"
            @uploaded="onUploaded"
        />
      </div>
    </div>

    <HeavyJobStatusPanel
        :job-id="heavyJob.jobId.value"
        :progress="heavyJob.progress.value"
        :reconnecting="heavyJob.reconnecting.value"
        :result="heavyJob.result.value"
        :sse-failed="heavyJob.sseFailed.value"
        :upload-error="uploadError"
        idle-prompt="파일을 업로드하면 처리가 시작됩니다"
    />
  </div>
</template>

<script lang="ts" setup>
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import FileUploader from './FileUploader.vue'
import HeavyJobStatusPanel from './HeavyJobStatusPanel.vue'
import {useHeavyJob} from '../composables/useHeavyJob'
import {isBatchResult, type UploadResult} from '../types'

const WATERMARK_POSITIONS = ['CENTER', 'TOP_LEFT', 'TOP_RIGHT', 'BOTTOM_LEFT', 'BOTTOM_RIGHT'] as const

interface EditorMode {
  id: 'watermark' | 'password' | 'header-footer'
  label: string
  moduleId: string
  fileAccept: string
  fileMultiple: boolean
  reorderable: boolean
}

const MODES: EditorMode[] = [
  {id: 'watermark', label: '워터마크 삽입', moduleId: 'pdf-watermark', fileAccept: '.pdf,.jpg,.jpeg,.png', fileMultiple: true, reorderable: true},
  {id: 'password', label: 'PDF 비밀번호 설정/해제', moduleId: 'pdf-password', fileAccept: '.pdf', fileMultiple: false, reorderable: false},
  {id: 'header-footer', label: '헤더/푸터/페이지번호', moduleId: 'pdf-header-footer', fileAccept: '.pdf', fileMultiple: false, reorderable: false},
]

const route = useRoute()
const router = useRouter()

const initialMode = typeof route.query.mode === 'string' && MODES.some(m => m.id === route.query.mode)
    ? route.query.mode as EditorMode['id']
    : 'watermark'
const mode = ref<EditorMode['id']>(initialMode)
const currentMode = computed(() => MODES.find(m => m.id === mode.value) ?? MODES[0])

// URL query 양방향 동기화 (code-gen과 동일 패턴, replace라 뒤로가기 이력을 오염시키지 않음)
watch(mode, id => {
  if (route.query.mode === id) return
  router.replace({query: {...route.query, mode: id}})
})
watch(() => route.query.mode, q => {
  if (typeof q === 'string' && q !== mode.value && MODES.some(m => m.id === q)) {
    mode.value = q as EditorMode['id']
  }
})

const watermarkText = ref('')
const watermarkPosition = ref<typeof WATERMARK_POSITIONS[number]>('CENTER')
const watermarkOpacity = ref('30')

const passwordMode = ref<'SET' | 'REMOVE'>('SET')
const password = ref('')

const headerText = ref('')
const footerText = ref('')
const pageNumberFormat = ref('')

// FileUploader가 값이 ''인 파라미터는 전송하지 않으므로(uploadErrorMessage 참고), 항상 같은 키
// 집합을 채워두고 현재 모드에 해당하지 않는 키는 빈 문자열로 둔다 — 모드별로 다른 형태의 객체를
// 반환하면 TS가 Record<string, string>으로 통일하지 못한다.
const EMPTY_PARAMS: Record<string, string> = {
  text: '', position: '', opacity: '', mode: '', password: '', headerText: '', footerText: '', pageNumberFormat: '',
}
const currentParams = computed<Record<string, string>>(() => {
  if (mode.value === 'watermark') {
    return {...EMPTY_PARAMS, text: watermarkText.value, position: watermarkPosition.value, opacity: watermarkOpacity.value}
  }
  if (mode.value === 'password') {
    return {...EMPTY_PARAMS, mode: passwordMode.value, password: password.value}
  }
  return {...EMPTY_PARAMS, headerText: headerText.value, footerText: footerText.value, pageNumberFormat: pageNumberFormat.value}
})

const heavyJob = useHeavyJob()
const uploadError = ref<string | null>(null)

watch(mode, () => {
  uploadError.value = ''
  heavyJob.reset()
})

function onUploaded(r: UploadResult) {
  uploadError.value = ''
  if (isBatchResult(r)) return // 이 페이지의 세 모듈은 항상 단건 job으로만 라우팅된다
  heavyJob.track(r.jobId)
}

function onUploadError(message: string) {
  uploadError.value = message
}
</script>
