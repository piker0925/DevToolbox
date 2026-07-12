<template>
  <div
      class="grid min-h-[420px] grid-cols-1 divide-y divide-border overflow-hidden rounded-xl border border-border bg-card lg:grid-cols-2 lg:divide-x lg:divide-y-0"
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

      <div class="flex flex-col gap-1.5 border-b border-border p-4">
        <label class="text-[11px] font-medium text-muted-foreground">변환 방식</label>
        <select
            v-model="modeId"
            class="rounded-md border border-input bg-background px-3 py-2 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
        >
          <option v-for="m in MODES" :key="m.id" :value="m.id">{{ m.label }}</option>
        </select>
      </div>

      <textarea
          v-model="input"
          :placeholder="mode.sample"
          class="min-h-[32vh] flex-1 resize-y bg-muted/40 p-4 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
      />

      <div class="flex h-9 shrink-0 items-center border-t border-border px-4">
        <p class="text-[11px] text-muted-foreground/70">입력하면 자동으로 변환됩니다</p>
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
            v-if="output"
            :class="copied ? 'text-emerald-500' : 'text-muted-foreground/60 hover:text-foreground'"
            class="rounded p-0.5 transition-colors"
            @click="copyOutput"
        >
          <Check v-if="copied" class="size-3.5"/>
          <Copy v-else class="size-3.5"/>
        </button>
      </div>

      <div class="flex-1 overflow-auto">
        <div v-if="error" class="flex h-full flex-col items-center justify-center gap-2 px-6 text-center">
          <AlertCircle class="size-5 text-destructive/70"/>
          <p class="text-[13px] text-foreground">{{ error }}</p>
          <p class="text-[11px] text-muted-foreground">입력 형식을 확인해 주세요</p>
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
</template>

<script lang="ts" setup>
import {computed, onUnmounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {AlertCircle, ArrowRight, Check, Copy, Loader2, Wand2} from 'lucide-vue-next'
import {apiClient} from '../api/client'
import {decodeBase64, decodeUrl, encodeBase64, encodeUrl} from '../utils/frontendTools'
import {Button} from '@/components/ui/button'

interface EncoderMode {
  id: string
  label: string
  sample: string
  /** 로컬 변환 함수. 없으면 백엔드 html-entity 모듈을 호출한다 */
  fn?: (input: string) => string
  backendMode?: 'encode' | 'decode'
}

const MODES: EncoderMode[] = [
  {id: 'base64-encode', label: 'Base64 인코드', fn: encodeBase64, sample: 'hello world 안녕하세요'},
  {id: 'base64-decode', label: 'Base64 디코드', fn: decodeBase64, sample: 'aGVsbG8gd29ybGQ='},
  {id: 'url-encode', label: 'URL 인코드', fn: encodeUrl, sample: 'https://example.com/검색?q=개발 도구'},
  {id: 'url-decode', label: 'URL 디코드', fn: decodeUrl, sample: 'https%3A%2F%2Fexample.com%2F%EA%B2%80%EC%83%89'},
  {
    id: 'html-encode',
    label: 'HTML Entity 인코드',
    backendMode: 'encode',
    sample: '<div class="greeting">hello & world</div>'
  },
  {
    id: 'html-decode',
    label: 'HTML Entity 디코드',
    backendMode: 'decode',
    sample: '&lt;div&gt;hello &amp; world&lt;/div&gt;'
  },
]

const route = useRoute()

const initialMode = typeof route.query.mode === 'string' && MODES.some(m => m.id === route.query.mode)
    ? route.query.mode
    : 'base64-encode'

const modeId = ref(initialMode)
const input = ref('')
const output = ref('')
const error = ref('')
const running = ref(false)
const copied = ref(false)

const mode = computed(() => MODES.find(m => m.id === modeId.value) ?? MODES[0])

let debounceTimer: ReturnType<typeof setTimeout> | null = null
let runToken = 0

watch([input, modeId], () => {
  if (!input.value) {
    output.value = ''
    error.value = ''
    return
  }
  if (mode.value.fn) {
    // 로컬 변환은 즉시 실행
    try {
      output.value = mode.value.fn(input.value)
      error.value = ''
    } catch {
      output.value = ''
      error.value = '변환할 수 없는 입력입니다'
    }
    return
  }
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runBackend, 500)
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

async function runBackend() {
  if (!mode.value.backendMode || !input.value) return
  const token = ++runToken
  running.value = true
  try {
    const {data} = await apiClient.post('/api/v1/tools/html-entity/run', {
      text: input.value,
      mode: mode.value.backendMode,
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
  input.value = mode.value.sample
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
