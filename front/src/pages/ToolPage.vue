<template>
  <!-- Loading -->
  <div v-if="loading" class="flex items-center gap-2 p-6 text-sm text-slate-400">
    <Loader2 class="size-4 animate-spin"/>
    불러오는 중...
  </div>

  <!-- Not found -->
  <div v-else-if="!mod" class="p-6 text-sm text-slate-500">모듈을 찾을 수 없습니다.</div>

  <!-- Module -->
  <template v-else>
    <!-- Sticky header bar -->
    <div class="sticky top-0 z-10 flex h-12 items-center border-b border-slate-100 bg-white px-6 gap-3">
      <nav class="flex items-center gap-1.5 text-[13px] text-slate-400">
        <router-link class="transition-colors hover:text-slate-600" to="/">홈</router-link>
        <ChevronRight class="size-3.5"/>
        <span class="text-slate-500">{{ mod.category }}</span>
        <ChevronRight class="size-3.5"/>
        <span class="font-medium text-slate-800">{{ mod.name }}</span>
      </nav>
      <span
          :class="mod.isHeavy
          ? 'bg-amber-100 text-amber-800'
          : 'bg-green-100 text-green-800'"
          class="rounded-full px-2 py-0.5 text-[10px] font-bold uppercase"
      >{{ mod.isHeavy ? 'Heavy' : 'Light' }}</span>
      <p v-if="mod.description" class="ml-auto text-[12px] text-slate-400 hidden lg:block">{{ mod.description }}</p>
    </div>

    <!-- Heavy: 2-panel layout -->
    <div v-if="mod.isHeavy" class="grid h-[calc(100vh-3rem)] grid-cols-2 divide-x divide-slate-200">
      <!-- Left: Upload -->
      <div class="flex flex-col">
        <div class="flex h-10 shrink-0 items-center border-b border-slate-100 px-4">
          <span class="text-[11px] font-medium text-slate-400">파일 업로드</span>
          <button
              v-if="jobId || result"
              class="ml-auto rounded p-0.5 text-slate-300 transition-colors hover:text-slate-500"
              @click="resetAll"
          >
            <X class="size-3.5"/>
          </button>
        </div>
        <div class="flex flex-1 flex-col p-6">
          <FileUploader :moduleId="mod.id" @uploaded="onUploaded"/>
        </div>
      </div>

      <!-- Right: Status / Result -->
      <div class="flex flex-col">
        <div class="flex h-10 shrink-0 items-center border-b border-slate-100 px-4">
          <span class="text-[11px] font-medium text-slate-400">결과</span>
        </div>
        <div class="flex flex-1 items-center justify-center p-6">
          <!-- Idle -->
          <div v-if="!jobId && !result" class="flex flex-col items-center gap-3 text-center">
            <div class="flex size-12 items-center justify-center rounded-xl border-2 border-dashed border-slate-200">
              <ArrowRight class="size-5 text-slate-300"/>
            </div>
            <p class="text-[12px] text-slate-400">파일을 업로드하면 처리가 시작됩니다</p>
          </div>
          <!-- Processing -->
          <div v-else-if="!result" class="flex flex-col items-center gap-4">
            <Loader2 class="size-8 animate-spin text-indigo-400"/>
            <p class="text-[13px] text-slate-500">처리 중입니다...</p>
            <JobPoller :jobId="jobId!" @done="onDone" @failed="onFailed"/>
          </div>
          <!-- Result -->
          <div v-else class="flex w-full flex-col gap-4">
            <ResultViewer :text="result.text" :url="result.url"/>
            <Button class="w-fit" variant="outline" @click="resetAll">다시 실행</Button>
          </div>
        </div>
      </div>
    </div>

    <!-- Light: full-height 2-panel -->
    <div v-else class="grid grid-cols-2 h-[calc(100vh-3rem)]">
      <!-- Input panel -->
      <div class="flex flex-col border-r border-slate-200">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-slate-100 px-4">
          <span class="text-[11px] font-medium text-slate-400">입력</span>
          <button
              v-if="runInput"
              class="rounded p-0.5 text-slate-300 transition-colors hover:text-slate-500"
              @click="resetLight"
          >
            <X class="size-3.5"/>
          </button>
        </div>

        <textarea
            v-model="runInput"
            class="flex-1 resize-none bg-slate-50 p-4 font-mono text-[13px] text-slate-800 outline-none placeholder:text-slate-300"
            placeholder="입력값을 입력하세요"
            @keydown="handleTextareaKeydown"
        />

        <div class="flex shrink-0 h-12 items-center gap-3 border-t border-slate-100 px-4">
          <Button :disabled="running" class="flex-1 h-8 text-[13px]" @click="runLight">
            <Loader2 v-if="running" class="size-3.5 animate-spin"/>
            <span>{{ running ? '실행 중...' : '실행' }}</span>
          </Button>
          <span class="text-[10px] text-slate-400 font-mono">⌘↵</span>
        </div>
      </div>

      <!-- Output panel -->
      <div class="flex flex-col">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-slate-100 px-4">
          <span class="text-[11px] font-medium text-slate-400">결과</span>
          <button
              v-if="result?.text"
              :class="copied ? 'text-emerald-500' : 'text-slate-300 hover:text-slate-500'"
              class="rounded p-0.5 transition-colors"
              @click="copyResult"
          >
            <Check v-if="copied" class="size-3.5"/>
            <Copy v-else class="size-3.5"/>
          </button>
        </div>

        <div class="flex-1 overflow-auto">
          <!-- Error state -->
          <div v-if="runError" class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
            <div class="flex size-10 items-center justify-center rounded-full bg-red-50">
              <AlertCircle class="size-5 text-red-400"/>
            </div>
            <div>
              <p class="text-[13px] font-medium text-slate-700">서버에 연결할 수 없습니다</p>
              <p class="mt-0.5 text-[11px] text-slate-400">잠시 후 다시 시도해 주세요</p>
            </div>
            <Button class="text-[12px]" size="sm" variant="outline" @click="runLight">다시 시도</Button>
          </div>

          <!-- Result -->
          <div v-else-if="result" class="p-4">
            <ResultViewer :text="result.text" :url="result.url"/>
          </div>

          <!-- Empty state -->
          <div v-else class="flex h-full flex-col items-center justify-center gap-2.5 px-6 text-center">
            <div class="flex size-12 items-center justify-center rounded-xl border-2 border-dashed border-slate-200">
              <ArrowRight class="size-5 text-slate-300"/>
            </div>
            <p class="text-[12px] text-slate-400">
              입력 후 <kbd class="rounded bg-slate-100 px-1 py-0.5 font-mono text-[10px]">⌘↵</kbd> 또는 실행 버튼을 누르세요
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Comments -->
    <div class="border-t border-slate-100 px-6 py-8">
      <h3 class="mb-6 text-[13px] font-semibold text-slate-700">커뮤니티</h3>
      <CommentSection :module-id="(route.params.moduleId as string)"/>
    </div>
  </template>
</template>

<script lang="ts" setup>
import {ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {AlertCircle, ArrowRight, Check, ChevronRight, Copy, Loader2, X} from 'lucide-vue-next'
import {apiClient} from '../api/client'
import {MOCK_MODULES} from '../api/mock'
import type {Job, Module, UploadResult} from '../types'
import {Button} from '@/components/ui/button'
import FileUploader from '../components/FileUploader.vue'
import JobPoller from '../components/JobPoller.vue'
import ResultViewer from '../components/ResultViewer.vue'
import CommentSection from '../components/CommentSection.vue'

interface RunResult {
  url: string | null;
  text: string | null
}

const route = useRoute()
const mod = ref<Module | null>(null)
const loading = ref(true)
const jobId = ref<string | null>(null)
const result = ref<RunResult | null>(null)
const runInput = ref('')
const running = ref(false)
const runError = ref('')
const copied = ref(false)

async function loadModule(moduleId: string) {
  loading.value = true
  mod.value = null
  resetAll()
  try {
    const {data} = await apiClient.get<Module[]>('/api/v1/modules')
    mod.value = data.find(m => m.id === moduleId) ?? null
  } catch {
    mod.value = MOCK_MODULES.find(m => m.id === moduleId) ?? null
  } finally {
    loading.value = false
  }
}

watch(() => route.params.moduleId as string, loadModule, {immediate: true})

function onUploaded(r: UploadResult) {
  jobId.value = r.jobId
}

function onDone(job: Job) {
  result.value = {url: job.resultUrl ?? null, text: job.resultText ?? null}
}

function onFailed(_job: Job) {
  result.value = {url: null, text: '처리에 실패했습니다.'}
}

async function runLight() {
  if (running.value) return
  running.value = true
  runError.value = ''
  result.value = null
  try {
    let params: Record<string, string> = {}
    try {
      params = JSON.parse(runInput.value)
    } catch {
      params = {input: runInput.value}
    }
    const {data} = await apiClient.post('/api/v1/run', {moduleId: mod.value?.id, params})
    result.value = {url: data.resultUrl ?? null, text: data.resultText ?? null}
  } catch {
    runError.value = '서버가 준비 중입니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    running.value = false
  }
}

function handleTextareaKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    runLight()
  }
}

async function copyResult() {
  const text = result.value?.text
  if (!text) return
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}

function resetAll() {
  jobId.value = null
  result.value = null
  runInput.value = ''
  runError.value = ''
  copied.value = false
}

function resetLight() {
  result.value = null
  runInput.value = ''
  runError.value = ''
  copied.value = false
}
</script>
