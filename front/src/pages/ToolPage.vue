<template>
  <div v-if="loading" class="flex items-center gap-2 p-6 text-sm text-muted-foreground">
    <Loader2 class="size-4 animate-spin"/>
    불러오는 중...
  </div>

  <div v-else-if="!mod" class="p-6 text-sm text-muted-foreground">모듈을 찾을 수 없습니다.</div>

  <template v-else>
    <!-- Breadcrumb -->
    <div
        class="sticky top-0 z-10 flex h-11 items-center gap-3 border-b border-border bg-background/90 px-4 backdrop-blur sm:px-6">
      <nav class="flex min-w-0 items-center gap-1.5 text-[13px] text-muted-foreground">
        <router-link class="shrink-0 transition-colors hover:text-foreground" to="/">홈</router-link>
        <ChevronRight class="size-3.5 shrink-0"/>
        <span class="shrink-0">{{ mod.category }}</span>
        <ChevronRight class="size-3.5 shrink-0"/>
        <span class="truncate font-medium text-foreground">{{ mod.name }}</span>
      </nav>
    </div>

    <div class="mx-auto w-full max-w-[1440px] px-4 pb-10 sm:px-6">

      <!-- Title -->
      <div class="flex flex-wrap items-center gap-x-3 gap-y-2 pb-4 pt-6">
        <h1 class="text-lg font-semibold tracking-tight text-foreground">{{ mod.name }}</h1>
        <span
            v-if="mod.isHeavy"
            class="rounded-full border border-border bg-secondary px-2 py-0.5 font-mono text-[10px] font-medium text-muted-foreground"
        >파일 처리</span>
        <p v-if="mod.description" class="text-[13px] text-muted-foreground">{{ mod.description }}</p>
        <div class="ml-auto flex items-center gap-3 text-[12px] text-muted-foreground">
          <span class="flex items-center gap-1.5">
            <BarChart2 class="size-3.5"/>
            사용 <span class="font-mono">{{ stats?.useCount ?? 0 }}</span>회
          </span>
          <button
              :class="liked ? 'border-rose-300 text-rose-500' : 'border-border hover:border-rose-300 hover:text-rose-500'"
              class="flex items-center gap-1.5 rounded-full border px-3 py-0.5 font-mono text-[12px] transition-colors"
              @click="toggleLike"
          >
            <Heart :class="liked ? 'fill-rose-500 text-rose-500' : ''" class="size-3.5"/>
            {{ stats?.likeCount ?? 0 }}
          </button>
        </div>
      </div>

      <!-- 통합 도구 -->
      <UnifiedConvertPage v-if="mod.id === 'data-convert'"/>
      <UnifiedEncoderPage v-else-if="mod.id === 'encoder'"/>
      <UnifiedTextUtilsPage v-else-if="mod.id === 'text-utils'"/>

      <!-- Frontend-only -->
      <FrontendToolPage v-else-if="mod.isFrontendOnly" :moduleId="mod.id"/>

      <!-- Heavy -->
      <div
          v-else-if="mod.isHeavy"
          class="grid min-h-[420px] grid-cols-1 divide-y divide-border overflow-hidden rounded-xl border border-border bg-card lg:grid-cols-2 lg:divide-x lg:divide-y-0"
      >
        <!-- Left: Params + Upload -->
        <div class="flex flex-col overflow-hidden">
          <div class="flex h-10 shrink-0 items-center border-b border-border px-4">
            <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">파일 업로드</span>
            <button
                v-if="jobId || batchId || result"
                class="ml-auto rounded p-0.5 text-muted-foreground/60 transition-colors hover:text-foreground"
                @click="resetAll"
            >
              <X class="size-3.5"/>
            </button>
          </div>

          <!-- Heavy params (있을 때만) -->
          <div v-if="heavyConfig?.params.length" class="flex shrink-0 flex-col gap-3 border-b border-border p-4">
            <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">파라미터</span>
            <div v-for="p in heavyConfig.params" :key="p.key" class="flex flex-col gap-1">
              <label class="text-[11px] text-muted-foreground">{{ p.label }}</label>
              <input
                  v-if="p.type === 'text'"
                  v-model="heavyFormValues[p.key]"
                  :placeholder="p.placeholder ?? ''"
                  class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/50 focus:border-ring focus:ring-2 focus:ring-ring/20"
                  type="text"
              />
              <select
                  v-else-if="p.type === 'select'"
                  v-model="heavyFormValues[p.key]"
                  class="rounded-md border border-input bg-background px-3 py-1.5 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
              >
                <option v-for="opt in p.options" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>
          </div>

          <!-- 텍스트 직접 입력 (json-schema-to-dto, openapi-to-code) -->
          <div v-if="heavyConfig?.textInput" class="flex flex-col border-b border-border">
            <div class="flex h-9 shrink-0 items-center justify-between border-b border-border px-4">
              <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">{{
                  heavyConfig.textInput.label
                }}</span>
            </div>
            <textarea
                v-model="heavyTextContent"
                :placeholder="heavyConfig.textInput.placeholder"
                class="h-56 resize-none bg-muted/40 p-3 font-mono text-[12px] text-foreground outline-none placeholder:text-muted-foreground/40"
            />
            <div class="flex items-center gap-2 px-4 py-2">
              <Button :disabled="!heavyTextContent.trim()" class="h-7 text-[12px]" @click="uploadTextAsFile">
                텍스트로 생성
              </Button>
              <span class="text-[11px] text-muted-foreground">또는 아래에서 파일 업로드</span>
            </div>
          </div>

          <div class="flex flex-1 flex-col overflow-auto p-6">
            <FileUploader
                :accept="heavyConfig?.fileAccept"
                :moduleId="mod.id"
                :multiple="heavyConfig?.fileMultiple ?? true"
                :params="heavyFormValues"
                :reorderable="heavyConfig?.reorderable ?? false"
                @uploaded="onUploaded"
            />
          </div>
        </div>

        <!-- Right: Result -->
        <div class="flex flex-col">
          <div class="flex h-10 shrink-0 items-center border-b border-border px-4">
            <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">결과</span>
          </div>
          <div class="flex flex-1 items-center justify-center p-6">
            <!-- 배치: 여러 파일을 각각 처리 후 ZIP -->
            <div v-if="batchId" class="flex w-full flex-col items-center gap-4 text-center">
              <BatchPoller
                  v-if="!batchComplete"
                  :batchId="batchId"
                  @done="onBatchDone"
                  @progress="onBatchProgress"
              />
              <template v-if="!batchComplete">
                <Loader2 class="size-8 animate-spin text-primary/60"/>
                <p class="text-[13px] text-muted-foreground">
                  일괄 처리 중… {{ batchProgress?.doneCount ?? 0 }} / {{ batchProgress?.totalCount ?? 0 }}
                  <span v-if="batchProgress?.failCount" class="text-destructive">(실패 {{
                      batchProgress.failCount
                    }})</span>
                </p>
              </template>
              <template v-else>
                <p class="text-[13px] text-foreground">
                  완료 {{ batchProgress?.doneCount ?? 0 }} / {{ batchProgress?.totalCount ?? 0 }}
                  <span v-if="batchProgress?.failCount" class="text-destructive">(실패 {{
                      batchProgress.failCount
                    }})</span>
                </p>
                <a
                    :href="batchResultUrl"
                    class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-[13px] font-medium text-primary-foreground transition-opacity hover:opacity-90"
                    data-testid="batch-download"
                    download
                >ZIP 다운로드</a>
                <Button class="w-fit" variant="outline" @click="resetAll">다시 실행</Button>
              </template>
            </div>

            <div v-else-if="!jobId && !result" class="flex flex-col items-center gap-3 text-center">
              <div class="flex size-12 items-center justify-center rounded-xl border-2 border-dashed border-border">
                <ArrowRight class="size-5 text-muted-foreground/50"/>
              </div>
              <p class="text-[12px] text-muted-foreground">파일을 업로드하면 처리가 시작됩니다</p>
            </div>
            <div v-else-if="!result" class="flex flex-col items-center gap-4">
              <Loader2 class="size-8 animate-spin text-primary/60"/>
              <p class="text-[13px] text-muted-foreground">처리 중입니다...</p>
            </div>
            <div v-else class="flex w-full flex-col gap-4">
              <ResultViewer :text="result.text" :url="result.url"/>
              <Button class="w-fit" variant="outline" @click="resetAll">다시 실행</Button>
            </div>
          </div>
        </div>
      </div>

      <!-- Light -->
      <div
          v-else
          class="grid min-h-[420px] grid-cols-1 divide-y divide-border overflow-hidden rounded-xl border border-border bg-card lg:grid-cols-2 lg:divide-x lg:divide-y-0"
      >
        <!-- Input -->
        <div class="flex flex-col">
          <div class="flex h-10 shrink-0 items-center justify-between border-b border-border px-4">
            <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">입력</span>
            <div class="flex items-center gap-1">
              <button
                  v-if="moduleConfig?.sample"
                  class="flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-muted-foreground/70 transition-colors hover:text-primary"
                  @click="applySample"
              >
                <Wand2 class="size-3"/>
                예시
              </button>
              <button
                  v-if="hasInput"
                  class="rounded p-0.5 text-muted-foreground/60 transition-colors hover:text-foreground"
                  @click="resetLight"
              >
                <X class="size-3.5"/>
              </button>
            </div>
          </div>

          <!-- CONFIGS 기반 입력 -->
          <div v-if="moduleConfig" class="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
            <div v-for="p in moduleConfig.params" :key="p.key" class="flex flex-col gap-1.5">
              <label class="text-[11px] font-medium text-muted-foreground">{{ p.label }}</label>
              <textarea
                  v-if="p.type === 'textarea'"
                  v-model="formValues[p.key]"
                  :class="textareaCount > 1 ? 'min-h-[20vh]' : 'min-h-[36vh]'"
                  :placeholder="p.placeholder ?? ''"
                  class="flex-1 resize-y rounded-md border border-input bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-ring focus:ring-2 focus:ring-ring/20"
                  @keydown="handleTextareaKeydown"
              />
              <input
                  v-else-if="p.type === 'text'"
                  v-model="formValues[p.key]"
                  :placeholder="p.placeholder ?? ''"
                  class="rounded-md border border-input bg-muted/40 px-3 py-2 font-mono text-[13px] text-foreground outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-ring focus:ring-2 focus:ring-ring/20"
                  type="text"
                  @keydown="handleTextareaKeydown"
              />
              <select
                  v-else-if="p.type === 'select'"
                  v-model="formValues[p.key]"
                  class="rounded-md border border-input bg-background px-3 py-2 text-[13px] text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
              >
                <option v-for="opt in p.options" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>
          </div>

          <!-- 단일 textarea (CONFIGS 없는 모듈) -->
          <textarea
              v-else
              v-model="runInput"
              class="min-h-[40vh] flex-1 resize-y bg-muted/40 p-4 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
              placeholder="입력값을 입력하세요"
              @keydown="handleTextareaKeydown"
          />

          <div class="flex h-11 shrink-0 items-center gap-3 border-t border-border px-4">
            <p class="flex-1 text-[11px] text-muted-foreground/70">
              <template v-if="autoRunEnabled">입력하면 자동으로 실행됩니다</template>
              <template v-else>입력 후 실행 버튼을 누르세요</template>
            </p>
            <span class="font-mono text-[10px] text-muted-foreground/60">⌘↵</span>
            <Button :disabled="running" class="h-8 px-4 text-[13px]" size="sm" @click="() => runLight()">
              <Loader2 v-if="running" class="size-3.5 animate-spin"/>
              <span>{{ running ? '실행 중' : '실행' }}</span>
            </Button>
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
                v-if="result?.text && moduleConfig?.resultType !== 'image'"
                :class="copied ? 'text-emerald-500' : 'text-muted-foreground/60 hover:text-foreground'"
                class="rounded p-0.5 transition-colors"
                @click="copyResult"
            >
              <Check v-if="copied" class="size-3.5"/>
              <Copy v-else class="size-3.5"/>
            </button>
          </div>

          <div class="flex-1 overflow-auto">
            <div v-if="runError && !result"
                 class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
              <div class="flex size-10 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle class="size-5 text-destructive/70"/>
              </div>
              <div>
                <p class="text-[13px] font-medium text-foreground">{{ runError }}</p>
                <p class="mt-0.5 text-[11px] text-muted-foreground">입력을 확인하거나 잠시 후 다시 시도해 주세요</p>
              </div>
              <Button class="text-[12px]" size="sm" variant="outline" @click="() => runLight()">다시 시도</Button>
            </div>

            <div v-else-if="moduleConfig?.resultType === 'image' && result?.text"
                 class="flex flex-col items-center gap-4 p-6">
              <img :src="`data:image/png;base64,${result.text}`" alt="생성된 이미지"
                   class="max-w-full rounded border border-border bg-white shadow-sm"/>
              <Button class="text-[12px]" size="sm" variant="outline" @click="downloadImage">다운로드</Button>
            </div>

            <div v-else-if="result" class="flex h-full flex-col p-4">
              <p v-if="runError" class="mb-2 text-[11px] text-destructive/80">{{ runError }}</p>
              <ResultViewer :text="result.text" :url="result.url" class="flex-1"/>
            </div>

            <div v-else class="flex h-full flex-col items-center justify-center gap-2.5 px-6 text-center">
              <div class="flex size-12 items-center justify-center rounded-xl border-2 border-dashed border-border">
                <ArrowRight class="size-5 text-muted-foreground/50"/>
              </div>
              <p class="text-[12px] text-muted-foreground">
                <template v-if="autoRunEnabled">입력과 동시에 결과가 나타납니다</template>
                <template v-else>입력 후 <kbd class="rounded bg-muted px-1 py-0.5 font-mono text-[10px]">⌘↵</kbd> 또는 실행 버튼을
                  누르세요
                </template>
              </p>
              <Button
                  v-if="moduleConfig?.sample"
                  class="text-[12px]"
                  size="sm"
                  variant="outline"
                  @click="applySample"
              >
                <Wand2 class="size-3.5"/>
                예시로 실행해 보기
              </Button>
            </div>
          </div>
        </div>
      </div>

      <!-- Comments (접이식) -->
      <div class="mt-8 border-t border-border pt-4">
        <button
            class="flex items-center gap-2 text-[13px] font-medium text-muted-foreground transition-colors hover:text-foreground"
            @click="showComments = !showComments"
        >
          <MessageSquare class="size-4"/>
          댓글
          <span v-if="commentCount !== null" class="font-mono text-[11px]">{{ commentCount }}</span>
          <ChevronDown :class="showComments ? 'rotate-180' : ''" class="size-3.5 transition-transform"/>
        </button>
        <div v-show="showComments">
          <CommentSection :module-id="(route.params.moduleId as string)" @count="commentCount = $event"/>
        </div>
      </div>
    </div>
  </template>
</template>

<script lang="ts" setup>
import {computed, onUnmounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {
  AlertCircle,
  ArrowRight,
  BarChart2,
  Check,
  ChevronDown,
  ChevronRight,
  Copy,
  Heart,
  Loader2,
  MessageSquare,
  Wand2,
  X,
} from 'lucide-vue-next'
import {apiClient} from '../api/client'
import {MOCK_MODULES} from '../api/mock'
import {normalizeApiModules} from '../api/modules'
import {buildFallbackParams} from '../utils/lightParams'
import type {BatchProgress, Module, UploadResult} from '../types'
import {isBatchResult} from '../types'
import {Button} from '@/components/ui/button'
import {useRecentTools} from '../composables/useRecentTools'
import FrontendToolPage from '../components/FrontendToolPage.vue'
import UnifiedConvertPage from '../components/UnifiedConvertPage.vue'
import UnifiedEncoderPage from '../components/UnifiedEncoderPage.vue'
import UnifiedTextUtilsPage from '../components/UnifiedTextUtilsPage.vue'
import FileUploader from '../components/FileUploader.vue'
import BatchPoller from '../components/BatchPoller.vue'
import ResultViewer from '../components/ResultViewer.vue'
import CommentSection from '../components/CommentSection.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

// ── 파라미터 타입 ─────────────────────────────────────────────────────────

interface ParamDef {
  key: string
  label: string
  type: 'textarea' | 'text' | 'select'
  placeholder?: string
  options?: string[]
  default?: string
}

interface ModuleConfig {
  params: ParamDef[]
  resultType?: 'image'
  sample?: Record<string, string>
  textInput?: { label: string; placeholder: string; filename: string }
  fileAccept?: string
  fileMultiple?: boolean
  reorderable?: boolean
}

// ── Light 모듈 CONFIGS ────────────────────────────────────────────────────

const MODULE_CONFIGS: Record<string, ModuleConfig> = {
  // 이미지 생성기
  'qr-code': {
    params: [
      {key: 'content', label: 'URL 또는 텍스트', type: 'text', placeholder: 'https://example.com'},
      {key: 'size', label: '크기 (px)', type: 'text', placeholder: '300', default: '300'},
    ],
    resultType: 'image',
    sample: {content: 'https://github.com'},
  },
  'barcode': {
    params: [
      {key: 'content', label: '바코드 내용', type: 'text', placeholder: '1234567890'},
      {key: 'width', label: '너비 (px)', type: 'text', placeholder: '400', default: '400'},
      {key: 'height', label: '높이 (px)', type: 'text', placeholder: '120', default: '120'},
    ],
    resultType: 'image',
    sample: {content: '0123456789'},
  },

  // 보안
  'bcrypt': {
    params: [
      {key: 'password', label: '비밀번호', type: 'text', placeholder: '해시할 비밀번호 입력'},
      {key: 'rounds', label: 'Rounds (강도)', type: 'select', options: ['10', '11', '12', '13'], default: '10'},
    ],
    sample: {password: 'p@ssw0rd!'},
  },
  'rsa-key': {
    params: [
      {key: 'preset', label: '키 유형 / 크기', type: 'select', options: ['RSA-2048', 'RSA-4096', 'EC-256', 'EC-384', 'EC-521'], default: 'RSA-2048'},
    ],
  },
  'hmac': {
    params: [
      {key: 'text', label: '메시지', type: 'textarea', placeholder: 'HMAC 서명할 텍스트'},
      {key: 'key', label: '서명 키', type: 'text', placeholder: 'secret-key'},
      {key: 'algorithm', label: '알고리즘', type: 'select', options: ['HmacSHA256', 'HmacSHA512'], default: 'HmacSHA256'},
    ],
    sample: {text: 'hello world', key: 'secret-key'},
  },
  'aes': {
    params: [
      {key: 'text', label: '텍스트', type: 'textarea', placeholder: '암호화/복호화할 텍스트'},
      {key: 'key', label: 'AES 키', type: 'text', placeholder: '16·24·32자 키'},
      {key: 'mode', label: '모드', type: 'select', options: ['encrypt', 'decrypt'], default: 'encrypt'},
    ],
    sample: {text: '민감한 데이터', key: '0123456789abcdef'},
  },

  // 포맷터
  'sql-formatter': {
    params: [{key: 'sql', label: 'SQL', type: 'textarea', placeholder: 'SELECT * FROM users WHERE id = 1;'}],
    sample: {sql: 'SELECT u.id, u.name, o.total FROM users u JOIN orders o ON o.user_id = u.id WHERE o.total > 1000 ORDER BY o.total DESC;'},
  },
  'xml-formatter': {
    params: [
      {key: 'xml', label: 'XML', type: 'textarea', placeholder: '<root><item>1</item></root>'},
      {key: 'minify', label: '출력 형식', type: 'select', options: ['false', 'true'], default: 'false'},
    ],
    sample: {xml: '<root><item id="1"><name>foo</name></item><item id="2"><name>bar</name></item></root>'},
  },
  'html-entity': {
    params: [
      {key: 'text', label: '텍스트', type: 'textarea', placeholder: '<div>hello & world</div>'},
      {key: 'mode', label: '모드', type: 'select', options: ['encode', 'decode'], default: 'encode'},
    ],
    sample: {text: '<div class="greeting">hello & world</div>'},
  },

  // 변환기
  'json-yaml': {
    params: [
      {key: 'input', label: '입력', type: 'textarea', placeholder: '{"key": "value"}'},
      {key: 'direction', label: '변환 방향', type: 'select', options: ['json-to-yaml', 'yaml-to-json'], default: 'json-to-yaml'},
    ],
    sample: {input: '{"name": "DevToolbox", "tags": ["dev", "tools"], "active": true}'},
  },
  'json-toml': {
    params: [
      {key: 'input', label: '입력', type: 'textarea', placeholder: '{"key": "value"}'},
      {key: 'direction', label: '변환 방향', type: 'select', options: ['json-to-toml', 'toml-to-json'], default: 'json-to-toml'},
    ],
    sample: {input: '{"name": "DevToolbox", "tags": ["dev", "tools"], "active": true}'},
  },
  'json-xml': {
    params: [
      {key: 'input', label: '입력', type: 'textarea', placeholder: '{"key": "value"}'},
      {key: 'direction', label: '변환 방향', type: 'select', options: ['json-to-xml', 'xml-to-json'], default: 'json-to-xml'},
    ],
    sample: {input: '{"name": "DevToolbox", "tags": ["dev", "tools"], "active": true}'},
  },
  'csv-json': {
    params: [
      {key: 'input', label: '입력', type: 'textarea', placeholder: 'name,age\nAlice,30'},
      {key: 'direction', label: '변환 방향', type: 'select', options: ['csv-to-json', 'json-to-csv'], default: 'csv-to-json'},
    ],
    sample: {input: 'name,age\nAlice,30\nBob,25'},
  },

  // 텍스트
  'text-diff': {
    params: [
      {key: 'original', label: '원본 텍스트', type: 'textarea', placeholder: '원본 텍스트 입력...'},
      {key: 'revised', label: '수정된 텍스트', type: 'textarea', placeholder: '수정된 텍스트 입력...'},
    ],
    sample: {
      original: 'The quick brown fox\njumps over the lazy dog',
      revised: 'The quick red fox\nleaps over the lazy dog',
    },
  },
  'regex-tester': {
    params: [
      {key: 'pattern', label: '정규식 패턴', type: 'text', placeholder: '[a-z]+'},
      {key: 'text', label: '테스트 텍스트', type: 'textarea', placeholder: '검사할 텍스트 입력...'},
    ],
    sample: {pattern: '[a-z]+@[a-z]+\\.[a-z]{2,}', text: 'contact: alice@example.com, bob@test.org'},
  },
  'case-converter': {
    params: [
      {key: 'text', label: '텍스트', type: 'text', placeholder: 'myVariableName'},
      {key: 'from', label: 'From', type: 'select', options: ['camel', 'pascal', 'snake', 'kebab'], default: 'camel'},
      {key: 'to', label: 'To', type: 'select', options: ['camel', 'pascal', 'snake', 'kebab'], default: 'snake'},
    ],
    sample: {text: 'myVariableName'},
  },

  // 네트워크
  'url-parser': {
    params: [{key: 'url', label: 'URL', type: 'text', placeholder: 'https://example.com/path?q=1#hash'}],
    sample: {url: 'https://user@example.com:8443/path/to/page?q=devtoolbox&lang=ko#section'},
  },
  'subnet-calc': {
    params: [{key: 'cidr', label: 'CIDR 표기', type: 'text', placeholder: '192.168.1.0/24'}],
    sample: {cidr: '192.168.1.0/24'},
  },
  'html-fetch': {
    params: [{key: 'url', label: 'URL', type: 'text', placeholder: 'https://example.com'}],
  },

  // DevOps
  'cron': {
    params: [
      {key: 'expression', label: 'Cron 표현식', type: 'text', placeholder: '0 0 * * *'},
      {key: 'count', label: '다음 실행 횟수', type: 'text', placeholder: '5', default: '5'},
    ],
    sample: {expression: '*/15 9-18 * * 1-5'},
  },
  'docker-compose': {
    params: [{key: 'command', label: 'docker run 명령어', type: 'textarea', placeholder: 'docker run -p 8080:8080 -e ENV=prod nginx'}],
    sample: {command: 'docker run -d -p 8080:80 -e TZ=Asia/Seoul --name web nginx:alpine'},
  },

  // 유틸
  'totp': {
    params: [{key: 'secret', label: 'TOTP Secret (Base32)', type: 'text', placeholder: 'JBSWY3DPEHPK3PXP'}],
    sample: {secret: 'JBSWY3DPEHPK3PXP'},
  },
  'multi-hash': {
    params: [{key: 'text', label: '텍스트', type: 'textarea', placeholder: '해시를 생성할 텍스트'}],
    sample: {text: 'hello world'},
  },
}

// ── Heavy 모듈 CONFIGS ────────────────────────────────────────────────────

const HEAVY_CONFIGS: Record<string, ModuleConfig> = {
  'image-to-pdf': {
    params: [],
    fileAccept: '.jpg,.jpeg,.png',
  },
  'pdf-merge': {
    params: [],
    fileAccept: '.pdf',
    reorderable: true,
  },
  'pdf-split': {
    params: [],
    fileAccept: '.pdf',
  },
  'markdown-to-pdf': {
    params: [],
    fileAccept: '.md',
  },
  'gif-create': {
    params: [
      {key: 'delay', label: '프레임 간격 (ms)', type: 'text', placeholder: '100', default: '100'},
    ],
  },
  'image-resize': {
    params: [
      {key: 'width', label: '너비 (px)', type: 'text', placeholder: '800', default: '800'},
      {key: 'height', label: '높이 (px)', type: 'text', placeholder: '600', default: '600'},
    ],
    // 다중 파일 → 파일당 별도 Job(배치) → ZIP. 배치 진행률 UI로 소비한다.
  },
  'image-format': {
    params: [
      {key: 'targetFormat', label: '출력 포맷', type: 'select', options: ['png', 'jpg'], default: 'png'},
    ],
  },
  'json-schema-to-dto': {
    params: [
      {key: 'packageName', label: '패키지명', type: 'text', placeholder: 'com.example', default: 'com.generated'},
    ],
    textInput: {
      label: 'JSON Schema 직접 입력',
      placeholder: '{\n  "type": "object",\n  "properties": {\n    "id": { "type": "integer" },\n    "name": { "type": "string" }\n  }\n}',
      filename: 'schema.json',
    },
  },
  'openapi-to-code': {
    params: [
      {key: 'language', label: '출력 언어', type: 'select', options: ['java', 'kotlin', 'typescript'], default: 'java'},
    ],
    textInput: {
      label: 'OpenAPI 스펙 직접 입력',
      placeholder: 'openapi: "3.0.0"\ninfo:\n  title: My API\n  version: "1.0"',
      filename: 'spec.yaml',
    },
  },
  'vuln-scan': {
    params: [],
    fileAccept: '.gradle,.kts,.xml',
  },
}

// ── 상태 ──────────────────────────────────────────────────────────────────

interface RunResult {
  url: string | null
  text: string | null
}

const route = useRoute()
const mod = ref<Module | null>(null)
const loading = ref(true)
const jobId = ref<string | null>(null)
const batchId = ref<string | null>(null)
const batchProgress = ref<BatchProgress | null>(null)
const batchComplete = ref(false)
const result = ref<RunResult | null>(null)
const runInput = ref('')
const formValues = ref<Record<string, string>>({})
const heavyFormValues = ref<Record<string, string>>({})
const heavyTextContent = ref('')
const running = ref(false)
const runError = ref('')
const copied = ref(false)
const showComments = ref(false)
const commentCount = ref<number | null>(null)

const {record: recordRecent} = useRecentTools()

const moduleConfig = computed(() => mod.value ? MODULE_CONFIGS[mod.value.id] ?? null : null)
const heavyConfig = computed(() => mod.value ? HEAVY_CONFIGS[mod.value.id] ?? null : null)
const batchResultUrl = computed(() => batchId.value ? `${API_BASE}/api/v1/batches/${batchId.value}/result` : '')

const textareaCount = computed(() =>
    moduleConfig.value?.params.filter(p => p.type === 'textarea').length ?? 0,
)

const hasInput = computed(() => {
  if (moduleConfig.value) return Object.values(formValues.value).some(v => v !== '' && v !== undefined)
  return !!runInput.value
})

// ── 통계 ──────────────────────────────────────────────────────────────────

interface ToolStats {
  moduleId: string
  useCount: number
  likeCount: number
}

const stats = ref<ToolStats | null>(null)
const liked = ref(false)

async function loadStats(moduleId: string) {
  try {
    const {data} = await apiClient.get<ToolStats>(`/api/v1/tools/${moduleId}/stats`)
    stats.value = data
  } catch {
    stats.value = null
  }
}

async function toggleLike() {
  const moduleId = route.params.moduleId as string
  try {
    const {data} = await apiClient.post<ToolStats>(`/api/v1/tools/${moduleId}/like`)
    stats.value = data
    liked.value = true
  } catch {
    // 무시
  }
}

// ── 로드 & 초기화 ─────────────────────────────────────────────────────────

async function loadModule(moduleId: string) {
  loading.value = true
  mod.value = null
  stats.value = null
  liked.value = false
  showComments.value = false
  commentCount.value = null
  resetAll()
  try {
    const {data} = await apiClient.get<Module[]>('/api/v1/modules')
    const allModules = normalizeApiModules(data)
    mod.value = allModules.find(m => m.id === moduleId) ?? null
  } catch {
    mod.value = MOCK_MODULES.find(m => m.id === moduleId) ?? null
  } finally {
    loading.value = false
    initForm()
    loadStats(moduleId)
    if (mod.value) recordRecent(mod.value.id)
  }
}

// ── SSE ───────────────────────────────────────────────────────────────────

let eventSource: EventSource | null = null

watch(() => route.params.moduleId as string, loadModule, {immediate: true})

watch(jobId, (id) => {
  if (id) startSse(id)
})

onUnmounted(stopSse)

function startSse(id: string) {
  stopSse()
  const es = new EventSource(`${API_BASE}/api/v1/jobs/${id}/stream`)
  eventSource = es
  es.addEventListener('job-status-changed', (e: MessageEvent) => {
    const d = JSON.parse(e.data)
    if (d.status === 'DONE' || d.status === 'FAILED') {
      stopSse()
      if (d.status === 'DONE') onDone(id)
      else onFailed()
    }
  })
  es.onerror = stopSse
}

function stopSse() {
  eventSource?.close()
  eventSource = null
}

function initForm() {
  const lc = mod.value ? MODULE_CONFIGS[mod.value.id] : undefined
  if (lc) {
    const v: Record<string, string> = {}
    for (const p of lc.params) v[p.key] = p.default ?? ''
    formValues.value = v
  }
  const hc = mod.value ? HEAVY_CONFIGS[mod.value.id] : undefined
  if (hc) {
    const v: Record<string, string> = {}
    for (const p of hc.params) v[p.key] = p.default ?? ''
    heavyFormValues.value = v
  }
}

// ── Heavy ─────────────────────────────────────────────────────────────────

function onUploaded(r: UploadResult) {
  if (isBatchResult(r)) {
    // 배치: 단건 SSE를 시작하지 않도록 jobId는 null로 두고 배치 진행률로 진입한다.
    batchId.value = r.batchId
    return
  }
  jobId.value = r.jobId
}

function onBatchProgress(p: BatchProgress) {
  batchProgress.value = p
}

function onBatchDone(p: BatchProgress) {
  batchProgress.value = p
  batchComplete.value = true
}

async function uploadTextAsFile() {
  if (!mod.value || !heavyConfig.value?.textInput || !heavyTextContent.value.trim()) return
  const {filename} = heavyConfig.value.textInput
  const blob = new Blob([heavyTextContent.value], {type: 'text/plain'})
  const form = new FormData()
  form.append('files', new File([blob], filename))
  Object.entries(heavyFormValues.value).forEach(([k, v]) => { if (v) form.append(k, v) })
  const {data} = await apiClient.post<UploadResult>(`/api/v1/tools/${mod.value.id}/upload`, form)
  onUploaded(data)
}

async function onDone(id: string) {
  try {
    const {data} = await apiClient.get(`/api/v1/jobs/${id}/result`)
    result.value = {url: data.url ?? null, text: data.text ?? null}
  } catch {
    result.value = {url: null, text: '결과를 불러오지 못했습니다.'}
  }
}

function onFailed() {
  result.value = {url: null, text: '처리에 실패했습니다.'}
}

// ── Light ─────────────────────────────────────────────────────────────────

// 키 입력마다 외부 요청이 나가면 안 되는 모듈은 자동 실행에서 제외한다
const AUTO_RUN_DISABLED = new Set(['html-fetch'])

const autoRunEnabled = computed(() =>
    !!mod.value && !mod.value.isHeavy && !mod.value.isFrontendOnly
    && !AUTO_RUN_DISABLED.has(mod.value.id)
    && autoRunReady.value !== null,
)

// 자동 실행 트리거 기준: 첫 번째 자유 입력 필드(select 제외)가 비어있지 않을 때.
// select만 있는 모듈(rsa-key 등)은 자동 실행하지 않는다 (null).
const autoRunReady = computed<boolean | null>(() => {
  if (moduleConfig.value) {
    const primary = moduleConfig.value.params.find(p => p.type !== 'select')
    if (!primary) return null
    return (formValues.value[primary.key]?.trim() ?? '') !== ''
  }
  return runInput.value.trim() !== ''
})

let runToken = 0
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch([formValues, runInput], () => {
  if (!autoRunEnabled.value || !autoRunReady.value) return
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => runLight({auto: true}), 600)
}, {deep: true})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

async function runLight(opts: { auto?: boolean } = {}) {
  if (running.value && opts.auto) return
  const token = ++runToken
  running.value = true
  if (!opts.auto) {
    runError.value = ''
    result.value = null
  }
  try {
    let params: Record<string, string>
    if (moduleConfig.value) {
      params = {...formValues.value}
      // rsa-key: split "RSA-2048" → {keyType: "RSA", keySize: "2048"}
      if (mod.value?.id === 'rsa-key' && params.preset) {
        const [keyType, keySize] = params.preset.split('-')
        params = {keyType, keySize}
      }
    } else {
      params = buildFallbackParams(runInput.value)
    }
    const {data} = await apiClient.post(`/api/v1/tools/${mod.value?.id}/run`, params)
    if (token !== runToken) return
    result.value = {url: null, text: data.result ?? null}
    runError.value = ''
  } catch (e: unknown) {
    if (token !== runToken) return
    const err = e as { response?: { data?: { message?: string } } }
    runError.value = err.response?.data?.message ?? '서버에 연결할 수 없습니다'
    if (!opts.auto) result.value = null
  } finally {
    if (token === runToken) running.value = false
  }
}

function applySample() {
  const sample = moduleConfig.value?.sample
  if (!sample) return
  formValues.value = {...formValues.value, ...sample}
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
  setTimeout(() => { copied.value = false }, 2000)
}

function downloadImage() {
  const text = result.value?.text
  if (!text) return
  const a = document.createElement('a')
  a.href = `data:image/png;base64,${text}`
  a.download = `${mod.value?.id ?? 'image'}.png`
  a.click()
}

// ── 리셋 ──────────────────────────────────────────────────────────────────

function resetAll() {
  stopSse()
  jobId.value = null
  batchId.value = null
  batchProgress.value = null
  batchComplete.value = false
  result.value = null
  runInput.value = ''
  runError.value = ''
  copied.value = false
  heavyTextContent.value = ''
  initForm()
}

function resetLight() {
  result.value = null
  runInput.value = ''
  runError.value = ''
  copied.value = false
  initForm()
}
</script>
