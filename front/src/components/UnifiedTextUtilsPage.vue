<template>
  <div class="flex flex-col gap-4">
    <!-- 탭 -->
    <div class="flex flex-wrap gap-1 rounded-lg border border-border bg-card p-1">
      <button
          v-for="t in TABS"
          :key="t.id"
          :class="tab === t.id
          ? 'bg-primary text-primary-foreground'
          : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'"
          class="flex-1 rounded-md px-3 py-1.5 text-[13px] font-medium transition-colors"
          @click="tab = t.id"
      >{{ t.label }}
      </button>
    </div>

    <div
        class="grid min-h-[380px] grid-cols-1 divide-y divide-border overflow-hidden rounded-xl border border-border bg-card lg:grid-cols-2 lg:divide-x lg:divide-y-0"
    >
      <!-- Input -->
      <div class="flex flex-col">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-border px-4">
          <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">입력</span>
          <button
              class="flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-muted-foreground/70 transition-colors hover:text-primary"
              @click="applySample"
          >
            <Wand2 class="size-3"/>
            예시
          </button>
        </div>

        <!-- 케이스 변환: 방향 선택 -->
        <div v-if="tab === 'case'" class="flex gap-2 border-b border-border p-4">
          <div class="flex flex-1 flex-col gap-1.5">
            <label class="text-[11px] font-medium text-muted-foreground">From</label>
            <select
                v-model="caseFrom"
                class="rounded-md border border-input bg-background px-3 py-2 font-mono text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
            >
              <option v-for="c in CASES" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="flex flex-1 flex-col gap-1.5">
            <label class="text-[11px] font-medium text-muted-foreground">To</label>
            <select
                v-model="caseTo"
                class="rounded-md border border-input bg-background px-3 py-2 font-mono text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
            >
              <option v-for="c in CASES" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
        </div>

        <!-- 한영 변환: 방향 선택 -->
        <div v-if="tab === 'keyboard'" class="flex flex-col gap-1.5 border-b border-border p-4">
          <label class="text-[11px] font-medium text-muted-foreground">변환 방향</label>
          <select
              v-model="keyboardDirection"
              class="rounded-md border border-input bg-background px-3 py-2 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
          >
            <option value="en-ko">영타 → 한글 (dkssud → 안녕)</option>
            <option value="ko-en">한타 → 영문 (안녕 → dkssud)</option>
          </select>
        </div>

        <textarea
            v-model="input"
            :placeholder="currentTab.placeholder"
            class="min-h-[28vh] flex-1 resize-y bg-muted/40 p-4 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
        />

        <div class="flex h-9 shrink-0 items-center border-t border-border px-4">
          <p class="text-[11px] text-muted-foreground/70">입력하면 자동으로 실행됩니다</p>
        </div>
      </div>

      <!-- Output -->
      <div class="flex flex-col">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-border px-4">
          <span
              class="flex items-center gap-2 font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            결과
            <Loader2 v-if="running" class="size-3 animate-spin"/>
          </span>
          <button
              v-if="output && tab !== 'count'"
              :class="copied ? 'text-emerald-500' : 'text-muted-foreground/60 hover:text-foreground'"
              class="rounded p-0.5 transition-colors"
              @click="copyOutput"
          >
            <Check v-if="copied" class="size-3.5"/>
            <Copy v-else class="size-3.5"/>
          </button>
        </div>

        <div class="flex-1 overflow-auto">
          <!-- 글자 수: 통계 카드 -->
          <div v-if="tab === 'count' && charStats" class="grid grid-cols-3 gap-3 p-4">
            <div
                v-for="s in [
                  {label: '문자', value: charStats.chars},
                  {label: '단어', value: charStats.words},
                  {label: '바이트 (UTF-8)', value: charStats.bytes},
                ]"
                :key="s.label"
                class="flex flex-col items-center gap-1 rounded-lg border border-border bg-muted/40 py-4"
            >
              <span class="font-mono text-xl font-semibold text-foreground">{{ s.value.toLocaleString() }}</span>
              <span class="text-[11px] text-muted-foreground">{{ s.label }}</span>
            </div>
          </div>

          <div v-else-if="error" class="flex h-full flex-col items-center justify-center gap-2 px-6 text-center">
            <AlertCircle class="size-5 text-destructive/70"/>
            <p class="text-[13px] text-foreground">{{ error }}</p>
          </div>

          <pre
              v-else-if="output"
              class="h-full whitespace-pre-wrap break-all p-4 font-mono text-[13px] text-foreground"
          >{{ output }}</pre>

          <div v-else class="flex h-full flex-col items-center justify-center gap-2.5 px-6 text-center">
            <div class="flex size-12 items-center justify-center rounded-xl border-2 border-dashed border-border">
              <ArrowRight class="size-5 text-muted-foreground/50"/>
            </div>
            <p class="text-[12px] text-muted-foreground">입력과 동시에 결과가 나타납니다</p>
            <Button class="text-[12px]" size="sm" variant="outline" @click="applySample">
              <Wand2 class="size-3.5"/>
              예시로 실행해 보기
            </Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onUnmounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {AlertCircle, ArrowRight, Check, Copy, Loader2, Wand2} from 'lucide-vue-next'
import {apiClient} from '../api/client'
import {convertKeyboard, countChars, normalizeWhitespace} from '../utils/frontendTools'
import {Button} from '@/components/ui/button'

type TabId = 'case' | 'count' | 'keyboard' | 'whitespace'

const TABS: Array<{ id: TabId; label: string; placeholder: string; sample: string }> = [
  {id: 'case', label: '케이스 변환', placeholder: 'myVariableName', sample: 'myVariableName'},
  {id: 'count', label: '글자 수', placeholder: '글자 수를 셀 텍스트...', sample: 'DevToolbox는 개발에 필요한 도구를 한곳에 모았습니다.'},
  {id: 'keyboard', label: '한영 변환', placeholder: 'dkssudgktpdy', sample: 'dkssudgktpdy'},
  {
    id: 'whitespace',
    label: '공백 정규화',
    placeholder: '연속   공백과\t탭이  섞인   텍스트',
    sample: '연속   공백과\t탭이  섞인   텍스트\n\n\n그리고 빈 줄'
  },
]

const CASES = ['camel', 'pascal', 'snake', 'kebab']

const route = useRoute()

const initialTab = typeof route.query.tab === 'string' && TABS.some(t => t.id === route.query.tab)
    ? route.query.tab as TabId
    : 'case'

const tab = ref<TabId>(initialTab)
const input = ref('')
const output = ref('')
const error = ref('')
const running = ref(false)
const copied = ref(false)
const caseFrom = ref('camel')
const caseTo = ref('snake')
const keyboardDirection = ref<'en-ko' | 'ko-en'>('en-ko')

const currentTab = computed(() => TABS.find(t => t.id === tab.value) ?? TABS[0])
const charStats = computed(() => tab.value === 'count' && input.value ? countChars(input.value) : null)

watch(tab, () => {
  input.value = ''
  output.value = ''
  error.value = ''
})

let debounceTimer: ReturnType<typeof setTimeout> | null = null
let runToken = 0

watch([input, caseFrom, caseTo, keyboardDirection], () => {
  error.value = ''
  if (!input.value) {
    output.value = ''
    return
  }
  switch (tab.value) {
    case 'count':
      // charStats computed가 처리
      output.value = ''
      return
    case 'keyboard':
      output.value = convertKeyboard(input.value, keyboardDirection.value)
      return
    case 'whitespace':
      output.value = normalizeWhitespace(input.value)
      return
    case 'case':
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(runCaseConvert, 500)
  }
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

async function runCaseConvert() {
  if (!input.value.trim()) return
  const token = ++runToken
  running.value = true
  try {
    const {data} = await apiClient.post('/api/v1/tools/case-converter/run', {
      text: input.value,
      from: caseFrom.value,
      to: caseTo.value,
    })
    if (token !== runToken) return
    output.value = data.result ?? ''
    error.value = ''
  } catch (e: unknown) {
    if (token !== runToken) return
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message ?? '서버에 연결할 수 없습니다'
    output.value = ''
  } finally {
    if (token === runToken) running.value = false
  }
}

function applySample() {
  input.value = currentTab.value.sample
}

async function copyOutput() {
  if (!output.value) return
  await navigator.clipboard.writeText(output.value)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}
</script>
