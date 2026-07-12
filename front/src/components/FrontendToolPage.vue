<template>
  <div class="px-6 py-8 max-w-5xl mx-auto">

    <!-- UUID -->
    <div v-if="moduleId === 'uuid'" class="flex flex-col gap-4 max-w-lg mx-auto w-full">
      <div class="flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <label class="text-[11px] font-medium text-muted-foreground">개수</label>
          <select v-model="uuidCount"
                  class="rounded-lg border border-border bg-card px-2 py-1 text-[12px] text-foreground outline-none focus:border-ring">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <label class="flex items-center gap-1.5 cursor-pointer">
          <input v-model="uuidNoHyphen" class="rounded accent-primary" type="checkbox"/>
          <span class="text-[11px] text-muted-foreground">하이픈 제거</span>
        </label>
      </div>
      <div class="flex flex-col gap-1.5">
        <div v-for="(uid, i) in uuidList" :key="i"
             class="flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5">
          <span class="flex-1 font-mono text-[13px] text-foreground">{{ uid }}</span>
          <button class="rounded p-1 transition-colors text-muted-foreground/50 hover:text-foreground"
                  @click="copyText(uid)">
            <Copy class="size-3.5"/>
          </button>
        </div>
        <p v-if="uuidList.length === 0" class="text-[12px] text-muted-foreground py-2">생성 버튼을 클릭하세요</p>
      </div>
      <div class="flex gap-2">
        <button
            class="flex-1 rounded-xl bg-primary py-2.5 text-[14px] font-semibold text-primary-foreground transition-colors hover:opacity-90"
            @click="generateUuids()">생성
        </button>
        <button v-if="uuidList.length > 1"
                class="rounded-xl border border-border bg-card px-4 py-2.5 text-[13px] text-foreground/80 transition-colors hover:bg-accent"
                @click="copyText(uuidList.join('\n'))">전체 복사
        </button>
      </div>
    </div>

    <!-- Timestamp -->
    <div v-else-if="moduleId === 'timestamp'" class="flex flex-col gap-4 max-w-lg mx-auto w-full">
      <div class="flex flex-col gap-1.5">
        <label class="text-[11px] font-medium text-muted-foreground">Unix Timestamp (초)</label>
        <input v-model="tsUnix"
               class="rounded-xl border border-border bg-card px-4 py-2.5 font-mono text-[13px] text-foreground outline-none focus:border-ring"
               placeholder="예: 1700000000"
               type="number"
               @input="onUnixInput"/>
      </div>
      <div class="flex items-center justify-center">
        <ArrowUpDown class="size-4 text-muted-foreground/50"/>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-[11px] font-medium text-muted-foreground">날짜/시간 (ISO 8601)</label>
        <input v-model="tsDate"
               class="rounded-xl border border-border bg-card px-4 py-2.5 font-mono text-[13px] text-foreground outline-none focus:border-ring"
               placeholder="예: 2023-11-14T22:13:20.000Z"
               type="text"
               @input="onDateInput"/>
      </div>
      <p v-if="tsError" class="text-[11px] text-destructive/70">{{ tsError }}</p>
    </div>

    <!-- Color code -->
    <div v-else-if="moduleId === 'color-code'" class="flex flex-col gap-4 max-w-lg mx-auto w-full">
      <div class="flex flex-col gap-1.5">
        <label class="text-[11px] font-medium text-muted-foreground">HEX</label>
        <div class="flex items-center gap-2">
          <div :style="{ backgroundColor: colorInput }" class="size-9 rounded-lg border border-border"/>
          <input v-model="colorInput"
                 class="flex-1 rounded-xl border border-border bg-card px-4 py-2.5 font-mono text-[13px] text-foreground outline-none focus:border-ring"
                 placeholder="#ff0000"
                 type="text"
                 @input="computeColor"/>
        </div>
      </div>
      <div v-if="colorResult" class="flex flex-col gap-2 rounded-xl border border-border bg-card p-4">
        <div v-for="row in colorResult" :key="row.label" class="flex items-center justify-between">
          <span class="text-[11px] text-muted-foreground">{{ row.label }}</span>
          <div class="flex items-center gap-2">
            <span class="font-mono text-[13px] text-foreground">{{ row.value }}</span>
            <button class="rounded p-0.5 text-muted-foreground/50 transition-colors hover:text-foreground"
                    @click="copyText(row.value)">
              <Copy class="size-3"/>
            </button>
          </div>
        </div>
      </div>
      <p v-if="colorError" class="text-[11px] text-destructive/70">{{ colorError }}</p>
    </div>

    <!-- JSON Formatter -->
    <div v-else-if="moduleId === 'json-formatter'" class="flex flex-col gap-3 max-w-4xl mx-auto w-full">
      <!-- 옵션 바 -->
      <div class="flex items-center gap-3 flex-wrap">
        <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
          <button v-for="opt in [{ value: 'format', label: '포맷' }, { value: 'minify', label: '미니파이' }]" :key="opt.value"
                  :class="jsonMode === opt.value ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                  class="rounded-md px-3 py-1 text-[12px] font-medium transition-colors"
                  @click="jsonMode = opt.value; compute()">{{ opt.label }}
          </button>
        </div>
        <template v-if="jsonMode === 'format'">
          <div class="flex items-center gap-1.5">
            <span class="text-[11px] text-muted-foreground">들여쓰기</span>
            <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
              <button v-for="opt in jsonIndentOptions" :key="String(opt.value)"
                      :class="jsonIndent === opt.value ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                      class="rounded-md px-2.5 py-1 text-[12px] font-medium transition-colors"
                      @click="jsonIndent = opt.value; compute()">{{ opt.label }}
              </button>
            </div>
          </div>
        </template>
        <label class="flex items-center gap-1.5 cursor-pointer ml-auto">
          <input v-model="jsonSortKeys" class="accent-primary" type="checkbox" @change="compute()"/>
          <span class="text-[11px] text-muted-foreground">키 정렬 (A→Z)</span>
        </label>
      </div>

      <!-- 2-패널 -->
      <div class="grid grid-cols-2 gap-3">
        <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center justify-between border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">JSON 입력</span>
            <button v-if="input" class="rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors"
                    @click="input = ''; output = ''; error = ''">
              <X class="size-3.5"/>
            </button>
          </div>
          <textarea v-model="input"
                    class="h-64 resize-none bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
                    placeholder='{"key": "value"}'
                    @input="compute()"/>
        </div>
        <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center justify-between border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">결과</span>
            <button v-if="output"
                    :class="copied ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
                    class="rounded p-0.5 transition-colors"
                    @click="copyText(output)">
              <Check v-if="copied" class="size-3.5"/>
              <Copy v-else class="size-3.5"/>
            </button>
          </div>
          <div class="h-64 overflow-auto">
            <div v-if="error" class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
              <div class="flex size-8 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle class="size-4 text-destructive/70"/>
              </div>
              <p class="text-[12px] text-muted-foreground">{{ error }}</p>
            </div>
            <pre v-else-if="output"
                 class="p-3 font-mono text-[13px] text-foreground whitespace-pre-wrap break-all">{{ output }}</pre>
            <div v-else class="flex h-full flex-col items-center justify-center gap-2 text-center">
              <ArrowRight class="size-4 text-muted-foreground/40"/>
              <p class="text-[11px] text-muted-foreground/50">입력하면 바로 변환됩니다</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 크기 정보 -->
      <div v-if="input && output && !error" class="flex items-center gap-3 text-[11px] text-muted-foreground">
        <span>원본 <span class="font-mono text-foreground/80">{{ formatBytes(jsonInputBytes) }}</span></span>
        <span>→</span>
        <span>결과 <span class="font-mono text-foreground/80">{{ formatBytes(jsonOutputBytes) }}</span></span>
        <span :class="jsonOutputBytes < jsonInputBytes ? 'text-emerald-500' : 'text-muted-foreground'">
          ({{
            jsonOutputBytes < jsonInputBytes ? '' : '+'
          }}{{ jsonInputBytes > 0 ? Math.round((jsonOutputBytes - jsonInputBytes) / jsonInputBytes * 100) : 0 }}%)
        </span>
      </div>
    </div>

    <!-- JWT Decoder -->
    <div v-else-if="moduleId === 'jwt-decoder'" class="grid grid-cols-2 gap-4 max-w-5xl mx-auto w-full">
      <!-- 왼쪽: 입력 + 옵션 -->
      <div class="flex flex-col gap-3">
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">JWT 토큰</span>
            <button v-if="jwtInput"
                    class="ml-auto rounded p-0.5 text-muted-foreground/50 transition-colors hover:text-foreground"
                    @click="jwtInput = ''; jwtResult = null; jwtError = ''">
              <X class="size-3.5"/>
            </button>
          </div>
          <textarea v-model="jwtInput"
                    class="h-44 w-full resize-none bg-muted/40 p-3 font-mono text-[11px] text-foreground outline-none placeholder:text-muted-foreground/40 break-all"
                    placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    @input="decodeJwtInput"/>
        </div>
        <div class="rounded-xl border border-border bg-card p-4">
          <p class="mb-3 text-[11px] font-medium text-muted-foreground">표시 옵션</p>
          <div class="flex flex-col gap-2.5">
            <label v-for="opt in JWT_OPTIONS" :key="opt.key" class="flex items-start gap-2 cursor-pointer">
              <input v-model="(jwtOptions as any)[opt.key]" class="mt-0.5 accent-primary" type="checkbox"/>
              <div>
                <span class="text-[12px] text-foreground">{{ opt.label }}</span>
                <span class="ml-1.5 text-[10px] text-muted-foreground">{{ opt.desc }}</span>
              </div>
            </label>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 결과 -->
      <div class="flex flex-col gap-3">
        <div v-if="jwtError" class="rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3">
          <p class="text-[12px] text-destructive">{{ jwtError }}</p>
        </div>

        <template v-else-if="jwtResult">
          <!-- 알고리즘 배지 + 만료 상태 -->
          <div v-if="jwtOptions.showAlg || (jwtOptions.showExpiry && jwtExpiry)"
               class="flex items-center gap-2 flex-wrap">
            <template v-if="jwtOptions.showAlg">
              <span :class="algBadgeClass" class="rounded-full px-2.5 py-0.5 text-[11px] font-bold">{{
                  (jwtResult.header as any).alg ?? 'unknown'
                }}</span>
              <span class="text-[11px] text-muted-foreground">{{ (jwtResult.header as any).typ ?? 'JWT' }}</span>
            </template>
            <div v-if="jwtOptions.showExpiry && jwtExpiry" class="ml-auto flex items-center gap-1.5">
              <span
                  :class="jwtExpiry.isExpired ? 'bg-destructive/10 text-destructive' : 'bg-emerald-100 text-emerald-600'"
                    class="rounded-full px-2 py-0.5 text-[10px] font-semibold">{{
                  jwtExpiry.isExpired ? '만료됨' : '유효'
                }}</span>
              <span class="text-[11px] text-muted-foreground">{{ jwtExpiry.timeStr }}</span>
            </div>
          </div>

          <!-- 탭 분리 -->
          <template v-if="jwtOptions.separateTabs">
            <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
              <button v-for="tab in ['payload', 'header']" :key="tab"
                      :class="jwtActiveTab === tab ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                      class="flex-1 rounded-md py-1 text-[12px] font-medium transition-colors"
                      @click="jwtActiveTab = tab">
                {{ tab === 'header' ? '헤더' : '페이로드' }}
              </button>
            </div>
            <JwtPanel
                :data="(jwtActiveTab === 'header' ? jwtResult.header : jwtResult.payload) as Record<string,unknown>"
                :is-payload="jwtActiveTab === 'payload'" :show-claims="jwtOptions.showClaims"
                :show-expiry="jwtOptions.showExpiry" :show-raw="jwtOptions.showRaw" @copy="copyText"/>
          </template>

          <!-- 합쳐진 모드 -->
          <template v-else>
            <div>
              <p class="mb-1.5 text-[10px] font-semibold uppercase tracking-widest text-muted-foreground/50">Header</p>
              <JwtPanel :data="jwtResult.header as Record<string,unknown>" :is-payload="false" :show-claims="false"
                        :show-expiry="false" :show-raw="true" @copy="copyText"/>
            </div>
            <div>
              <p class="mb-1.5 text-[10px] font-semibold uppercase tracking-widest text-muted-foreground/50">Payload</p>
              <JwtPanel :data="jwtResult.payload as Record<string,unknown>" :is-payload="true"
                        :show-claims="jwtOptions.showClaims" :show-expiry="jwtOptions.showExpiry"
                        :show-raw="jwtOptions.showRaw" @copy="copyText"/>
            </div>
          </template>
        </template>

        <div v-else class="flex flex-col items-center justify-center gap-2 py-16 text-center">
          <ArrowRight class="size-4 text-muted-foreground/40"/>
          <p class="text-[11px] text-muted-foreground/50">JWT 토큰을 입력하세요</p>
        </div>
      </div>
    </div>

    <!-- Base64 -->
    <div v-else-if="moduleId === 'base64'" class="flex flex-col gap-3 max-w-4xl mx-auto w-full">
      <div class="flex items-center gap-3">
        <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
          <button v-for="opt in [{ value: 'encode', label: '인코딩' }, { value: 'decode', label: '디코딩' }]" :key="opt.value"
                  :class="direction === opt.value ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                  class="rounded-md px-3 py-1 text-[12px] font-medium transition-colors"
                  @click="direction = opt.value; compute()">{{ opt.label }}
          </button>
        </div>
        <label class="flex items-center gap-1.5 cursor-pointer ml-auto">
          <input v-model="base64UrlSafe" class="accent-primary" type="checkbox" @change="compute()"/>
          <span class="text-[11px] text-muted-foreground">URL-safe <span
              class="text-muted-foreground/50">(- _ 패딩 없음)</span></span>
        </label>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center justify-between border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">{{
                direction === 'encode' ? '텍스트' : 'Base64'
              }}</span>
            <button v-if="input" class="rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors"
                    @click="input = ''; output = ''; error = ''">
              <X class="size-3.5"/>
            </button>
          </div>
          <textarea v-model="input" :placeholder="direction === 'encode' ? '인코딩할 텍스트를 입력하세요' : 'Base64 문자열을 입력하세요'"
                    class="h-56 resize-none bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
                    @input="compute()"/>
        </div>
        <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center justify-between border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">결과</span>
            <button v-if="output"
                    :class="copied ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
                    class="rounded p-0.5 transition-colors"
                    @click="copyText(output)">
              <Check v-if="copied" class="size-3.5"/>
              <Copy v-else class="size-3.5"/>
            </button>
          </div>
          <div class="h-56 overflow-auto">
            <div v-if="error" class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
              <div class="flex size-8 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle class="size-4 text-destructive/70"/>
              </div>
              <p class="text-[12px] text-muted-foreground">{{ error }}</p>
            </div>
            <pre v-else-if="output"
                 class="p-3 font-mono text-[13px] text-foreground whitespace-pre-wrap break-all">{{ output }}</pre>
            <div v-else class="flex h-full flex-col items-center justify-center gap-2 text-center">
              <ArrowRight class="size-4 text-muted-foreground/40"/>
              <p class="text-[11px] text-muted-foreground/50">입력하면 바로 변환됩니다</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- URL 인코딩 -->
    <div v-else-if="moduleId === 'url-encode'" class="flex flex-col gap-3 max-w-4xl mx-auto w-full">
      <!-- 옵션 바 -->
      <div class="flex items-center gap-3 flex-wrap">
        <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
          <button v-for="opt in [{ value: 'transform', label: '변환' }, { value: 'query', label: '쿼리 파싱' }]"
                  :key="opt.value"
                  :class="urlMode === opt.value ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                  class="rounded-md px-3 py-1 text-[12px] font-medium transition-colors"
                  @click="urlMode = opt.value; input = ''; output = ''; error = ''; urlQueryParams = []">{{ opt.label }}
          </button>
        </div>
        <template v-if="urlMode === 'transform'">
          <div class="flex gap-0.5 rounded-lg bg-muted p-0.5">
            <button v-for="opt in [{ value: 'encode', label: '인코딩' }, { value: 'decode', label: '디코딩' }]"
                    :key="opt.value"
                    :class="direction === opt.value ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
                    class="rounded-md px-3 py-1 text-[12px] font-medium transition-colors"
                    @click="direction = opt.value; compute()">{{ opt.label }}
            </button>
          </div>
        </template>
      </div>

      <!-- 변환 모드: 2-패널 -->
      <template v-if="urlMode === 'transform'">
        <div class="grid grid-cols-2 gap-3">
          <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
            <div class="flex h-9 items-center justify-between border-b border-border px-3">
              <span class="text-[11px] font-medium text-muted-foreground">{{
                  direction === 'encode' ? '텍스트' : '인코딩된 텍스트'
                }}</span>
              <button v-if="input"
                      class="rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors"
                      @click="input = ''; output = ''; error = ''">
                <X class="size-3.5"/>
              </button>
            </div>
            <textarea v-model="input" :placeholder="direction === 'encode' ? '인코딩할 텍스트를 입력하세요' : '디코딩할 텍스트를 입력하세요'"
                      class="h-56 resize-none bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
                      @input="compute()"/>
          </div>
          <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
            <div class="flex h-9 items-center justify-between border-b border-border px-3">
              <span class="text-[11px] font-medium text-muted-foreground">결과</span>
              <button v-if="output"
                      :class="copied ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
                      class="rounded p-0.5 transition-colors"
                      @click="copyText(output)">
                <Check v-if="copied" class="size-3.5"/>
                <Copy v-else class="size-3.5"/>
              </button>
            </div>
            <div class="h-56 overflow-auto">
              <div v-if="error" class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
                <div class="flex size-8 items-center justify-center rounded-full bg-destructive/10">
                  <AlertCircle class="size-4 text-destructive/70"/>
                </div>
                <p class="text-[12px] text-muted-foreground">{{ error }}</p>
              </div>
              <pre v-else-if="output"
                   class="p-3 font-mono text-[13px] text-foreground whitespace-pre-wrap break-all">{{ output }}</pre>
              <div v-else class="flex h-full flex-col items-center justify-center gap-2 text-center">
                <ArrowRight class="size-4 text-muted-foreground/40"/>
                <p class="text-[11px] text-muted-foreground/50">입력하면 바로 변환됩니다</p>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 쿼리 파싱 모드 -->
      <template v-else>
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center justify-between border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">URL 또는 쿼리스트링</span>
            <button v-if="input" class="rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors"
                    @click="input = ''; urlQueryParams = []; error = ''">
              <X class="size-3.5"/>
            </button>
          </div>
          <textarea v-model="input"
                    class="h-24 w-full resize-none bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
                    placeholder="https://example.com/search?q=한글&page=1"
                    @input="computeUrlQuery()"/>
        </div>
        <div v-if="urlQueryParams.length > 0" class="rounded-xl border border-border bg-card overflow-hidden">
          <div class="flex h-9 items-center border-b border-border px-3">
            <span class="text-[11px] font-medium text-muted-foreground">파라미터</span>
            <span class="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground">{{
                urlQueryParams.length
              }}</span>
          </div>
          <div class="divide-y divide-border">
            <div v-for="(param, i) in urlQueryParams" :key="i" class="flex items-start gap-3 px-4 py-3">
              <span class="w-40 shrink-0 font-mono text-[12px] text-primary break-all">{{ param.key }}</span>
              <div class="flex-1 min-w-0">
                <p class="font-mono text-[12px] text-foreground break-all">{{ param.value }}</p>
                <p v-if="param.raw !== param.value"
                   class="mt-0.5 font-mono text-[10px] text-muted-foreground break-all">raw:
                  {{ param.raw }}</p>
              </div>
              <button class="shrink-0 rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors"
                      @click="copyText(param.value)">
                <Copy class="size-3.5"/>
              </button>
            </div>
          </div>
        </div>
        <div v-else-if="input && !error" class="rounded-xl border border-border bg-muted/40 px-4 py-8 text-center">
          <p class="text-[12px] text-muted-foreground">쿼리 파라미터를 찾을 수 없습니다</p>
        </div>
        <div v-if="error" class="rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3">
          <p class="text-[12px] text-destructive">{{ error }}</p>
        </div>
      </template>
    </div>

    <!-- 나머지 도구: 2-패널 자동 변환 -->
    <div v-else class="grid grid-cols-2 gap-4 max-w-4xl mx-auto w-full">
      <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-border px-3">
          <span class="text-[11px] font-medium text-muted-foreground">{{ config.inputLabel }}</span>
          <div class="flex items-center gap-1">
            <div v-if="config.hasToggle" class="flex gap-0.5">
              <button v-for="opt in config.toggleOptions" :key="opt.value"
                      :class="direction === opt.value ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:text-foreground'"
                      class="rounded px-2 py-0.5 text-[10px] font-medium transition-colors"
                      @click="direction = opt.value; compute()">{{ opt.label }}
              </button>
            </div>
            <div v-if="config.hasJsonToggle" class="flex gap-0.5">
              <button v-for="opt in [{ value: 'format', label: '포맷' }, { value: 'minify', label: '미니파이' }]"
                      :key="opt.value"
                      :class="jsonMode === opt.value ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:text-foreground'"
                      class="rounded px-2 py-0.5 text-[10px] font-medium transition-colors"
                      @click="jsonMode = opt.value; compute()">{{ opt.label }}
              </button>
            </div>
            <button v-if="input"
                    class="ml-1 rounded p-0.5 text-muted-foreground/50 transition-colors hover:text-foreground"
                    @click="input = ''; output = ''; error = ''">
              <X class="size-3.5"/>
            </button>
          </div>
        </div>
        <textarea v-model="input" :placeholder="config.placeholder"
                  class="h-56 resize-none bg-muted/40 p-3 font-mono text-[13px] text-foreground outline-none placeholder:text-muted-foreground/40"
                  @input="compute()"/>
      </div>
      <div class="flex flex-col rounded-xl border border-border bg-card overflow-hidden">
        <div class="flex h-10 shrink-0 items-center justify-between border-b border-border px-3">
          <span class="text-[11px] font-medium text-muted-foreground">{{ config.outputLabel }}</span>
          <button v-if="output" :class="copied ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
                  class="rounded p-0.5 transition-colors"
                  @click="copyText(output)">
            <Check v-if="copied" class="size-3.5"/>
            <Copy v-else class="size-3.5"/>
          </button>
        </div>
        <div class="h-56 overflow-auto">
          <div v-if="error" class="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
            <div class="flex size-8 items-center justify-center rounded-full bg-destructive/10">
              <AlertCircle class="size-4 text-destructive/70"/>
            </div>
            <p class="text-[12px] text-muted-foreground">{{ error }}</p>
          </div>
          <div v-else-if="moduleId === 'char-count' && stats"
               class="flex h-full flex-col items-center justify-center gap-3 px-4">
            <div class="grid w-full grid-cols-3 gap-3">
              <div v-for="stat in stats" :key="stat.label"
                   class="flex flex-col items-center gap-1 rounded-xl border border-border bg-muted/40 py-4">
                <span class="text-[22px] font-bold text-foreground">{{ stat.value }}</span>
                <span class="text-[10px] text-muted-foreground">{{ stat.label }}</span>
              </div>
            </div>
          </div>
          <pre v-else-if="output"
               class="p-3 font-mono text-[13px] text-foreground whitespace-pre-wrap break-all">{{ output }}</pre>
          <div v-else class="flex h-full flex-col items-center justify-center gap-2 px-6 text-center">
            <ArrowRight class="size-4 text-muted-foreground/40"/>
            <p class="text-[11px] text-muted-foreground/50">입력하면 바로 변환됩니다</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script lang="ts" setup>
import {computed, defineComponent, h, reactive, ref, watch} from 'vue'
import {AlertCircle, ArrowRight, ArrowUpDown, Check, Copy, X} from 'lucide-vue-next'
import {
  convertKeyboard,
  countChars,
  dateToUnix,
  decodeBase64,
  decodeBase64Url,
  decodeJwt,
  decodeUrl,
  encodeBase64,
  encodeBase64Url,
  encodeUrl,
  formatJson,
  generateUuid,
  hexToRgb,
  minifyJson,
  normalizeWhitespace,
  parseQueryString,
  rgbToHex,
  rgbToHsl,
  unixToDate,
} from '../utils/frontendTools'

const props = defineProps<{ moduleId: string }>()

// ── JWT 클레임 패널 (인라인 컴포넌트) ────────────────────────────────────────
const TIMESTAMP_CLAIMS = new Set(['exp', 'iat', 'nbf'])
const CLAIM_LABELS: Record<string, string> = {
  sub: 'Subject', iss: 'Issuer', aud: 'Audience',
  exp: 'Expiration', iat: 'Issued At', nbf: 'Not Before', jti: 'JWT ID',
  name: 'Name', email: 'Email', email_verified: 'Email Verified',
  given_name: 'Given Name', family_name: 'Family Name',
  preferred_username: 'Username', picture: 'Picture URL',
  locale: 'Locale', role: 'Role', roles: 'Roles',
  scope: 'Scope', azp: 'Authorized Party', nonce: 'Nonce', sid: 'Session ID',
}

function formatClaimValue(key: string, value: unknown, showExpiry: boolean): string {
  if (value === null) return 'null'
  if (typeof value === 'boolean') return value ? '✓ true' : '✗ false'
  if (TIMESTAMP_CLAIMS.has(key) && typeof value === 'number' && showExpiry)
    return `${value}  (${unixToDate(value)})`
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const JwtPanel = defineComponent({
  props: {
    data: {type: Object as () => Record<string, unknown>, required: true},
    isPayload: Boolean,
    showClaims: Boolean,
    showRaw: Boolean,
    showExpiry: Boolean,
  },
  emits: ['copy'],
  setup(props, {emit}) {
    const entries = computed(() => Object.entries(props.data))
    const knownEntries = computed(() => entries.value.filter(([k]) => CLAIM_LABELS[k]))
    const unknownEntries = computed(() => entries.value.filter(([k]) => !CLAIM_LABELS[k]))
    const rawJson = computed(() => JSON.stringify(props.data, null, 2))

    return () => {
      const children = []

      // 구조화 클레임 테이블
      if (props.showClaims && props.isPayload) {
        const rows = [
          ...knownEntries.value.map(([k, v]) =>
              h('div', {class: 'flex items-start gap-3 px-3 py-2 border-b border-border last:border-0'}, [
                h('div', {class: 'w-28 shrink-0'}, [
                  h('p', {class: 'text-[10px] font-medium text-muted-foreground'}, CLAIM_LABELS[k]),
                  h('p', {class: 'font-mono text-[9px] text-muted-foreground/50'}, k),
                ]),
                h('span', {class: 'flex-1 font-mono text-[11px] text-foreground break-all'}, formatClaimValue(k, v, props.showExpiry)),
                h('button', {
                      class: 'rounded p-0.5 text-muted-foreground/40 hover:text-muted-foreground transition-colors',
                      onClick: () => emit('copy', String(v))
                    },
                    h(Copy, {class: 'size-3'})
                ),
              ])
          ),
          ...(unknownEntries.value.length ? [
            h('div', {class: 'border-t border-dashed border-border'}),
            ...unknownEntries.value.map(([k, v]) =>
                h('div', {class: 'flex items-start gap-3 px-3 py-2 border-b border-border last:border-0'}, [
                  h('div', {class: 'w-28 shrink-0'}, h('p', {class: 'font-mono text-[10px] text-muted-foreground'}, k)),
                  h('span', {class: 'flex-1 font-mono text-[11px] text-foreground break-all'}, formatClaimValue(k, v, props.showExpiry)),
                  h('button', {
                        class: 'rounded p-0.5 text-muted-foreground/40 hover:text-muted-foreground transition-colors',
                        onClick: () => emit('copy', String(v))
                      },
                      h(Copy, {class: 'size-3'})
                  ),
                ])
            ),
          ] : []),
        ]
        children.push(h('div', {class: 'rounded-xl border border-border bg-card overflow-hidden'}, rows))
      }

      // Raw JSON
      if (props.showRaw || !props.isPayload || !props.showClaims) {
        children.push(
            h('div', {class: 'relative rounded-xl border border-border bg-muted/40 overflow-hidden'}, [
              h('button', {
                    class: 'absolute right-2 top-2 rounded p-0.5 text-muted-foreground/50 hover:text-foreground transition-colors',
                    onClick: () => emit('copy', rawJson.value)
                  },
                  h(Copy, {class: 'size-3'})
              ),
              h('pre', {class: 'p-3 font-mono text-[11px] text-foreground whitespace-pre-wrap break-all'}, rawJson.value),
            ])
        )
      }

      return h('div', {class: 'flex flex-col gap-2'}, children)
    }
  },
})

// ── 공통 상태 ────────────────────────────────────────────────────────────────
const input = ref('')
const output = ref('')
const error = ref('')
const direction = ref('encode')
const jsonMode = ref('format')
const copied = ref(false)

// ── UUID ─────────────────────────────────────────────────────────────────────
const uuidValue = ref('')
const uuidRawList = ref<string[]>([])
const uuidCount = ref(1)
const uuidNoHyphen = ref(false)

const uuidList = computed(() =>
    uuidRawList.value.map(id => uuidNoHyphen.value ? id.replace(/-/g, '') : id)
)

function generateUuids() {
  uuidRawList.value = Array.from({length: uuidCount.value}, () => generateUuid())
}

// ── Timestamp ────────────────────────────────────────────────────────────────
const tsUnix = ref('')
const tsDate = ref('')
const tsError = ref('')

function onUnixInput() {
  tsError.value = ''
  const n = Number(tsUnix.value)
  if (!tsUnix.value || isNaN(n)) {
    tsDate.value = '';
    return
  }
  try {
    tsDate.value = unixToDate(n)
  } catch {
    tsError.value = '올바른 Unix timestamp가 아닙니다.'
  }
}

function onDateInput() {
  tsError.value = ''
  if (!tsDate.value) {
    tsUnix.value = '';
    return
  }
  try {
    const unix = dateToUnix(tsDate.value)
    if (isNaN(unix)) throw new Error()
    tsUnix.value = String(unix)
  } catch {
    tsError.value = '올바른 날짜 형식이 아닙니다.'
  }
}

// ── Color ─────────────────────────────────────────────────────────────────────
const colorInput = ref('')
const colorError = ref('')
const colorResult = ref<{ label: string; value: string }[] | null>(null)

function computeColor() {
  colorError.value = ''
  colorResult.value = null
  if (!colorInput.value) return
  try {
    const hex = colorInput.value.startsWith('#') ? colorInput.value : `#${colorInput.value}`
    const {r, g, b} = hexToRgb(hex)
    const {h, s, l} = rgbToHsl(r, g, b)
    colorResult.value = [
      {label: 'HEX', value: rgbToHex(r, g, b)},
      {label: 'RGB', value: `rgb(${r}, ${g}, ${b})`},
      {label: 'HSL', value: `hsl(${h}, ${s}%, ${l}%)`},
    ]
  } catch {
    colorError.value = '올바른 HEX 색상을 입력하세요. 예: #ff0000'
  }
}

// ── URL 인코딩 ────────────────────────────────────────────────────────────────
const urlMode = ref<'transform' | 'query'>('transform')
const urlQueryParams = ref<{ key: string; value: string; raw: string }[]>([])

function computeUrlQuery() {
  error.value = ''
  urlQueryParams.value = []
  if (!input.value) return
  try {
    urlQueryParams.value = parseQueryString(input.value)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '파싱 오류가 발생했습니다.'
  }
}

// ── Base64 ───────────────────────────────────────────────────────────────────
const base64UrlSafe = ref(false)

// ── JSON Formatter ────────────────────────────────────────────────────────────
const jsonIndent = ref<number | string>(2)
const jsonSortKeys = ref(false)
const jsonIndentOptions = [
  {value: 2, label: '2'},
  {value: 4, label: '4'},
  {value: '\t', label: '탭'},
]

const jsonInputBytes = computed(() => new TextEncoder().encode(input.value).length)
const jsonOutputBytes = computed(() => new TextEncoder().encode(output.value).length)

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

// ── JWT ───────────────────────────────────────────────────────────────────────
const jwtInput = ref('')
const jwtResult = ref<{ header: unknown; payload: unknown } | null>(null)
const jwtError = ref('')
const jwtActiveTab = ref('payload')

const jwtOptions = reactive({
  showAlg: true,
  showExpiry: true,
  showClaims: true,
  showRaw: false,
  separateTabs: false,
})

const JWT_OPTIONS = [
  {key: 'showAlg', label: '알고리즘 배지', desc: 'alg, typ 표시'},
  {key: 'showExpiry', label: '만료 시간 파싱', desc: 'exp/iat/nbf → 날짜 변환'},
  {key: 'showClaims', label: '클레임 구조화', desc: '표준 클레임 라벨 + 테이블'},
  {key: 'showRaw', label: 'Raw JSON', desc: '포맷된 JSON 원문'},
  {key: 'separateTabs', label: '헤더/페이로드 탭 분리', desc: '탭으로 전환'},
]

function decodeJwtInput() {
  jwtError.value = ''
  jwtResult.value = null
  if (!jwtInput.value.trim()) return
  try {
    jwtResult.value = decodeJwt(jwtInput.value.trim())
  } catch (e: unknown) {
    jwtError.value = e instanceof Error ? e.message : '유효하지 않은 JWT입니다.'
  }
}

const algBadgeClass = computed(() => {
  const alg = String((jwtResult.value?.header as Record<string, unknown>)?.alg ?? '')
  if (!alg || alg === 'none') return 'bg-destructive/10 text-destructive'
  if (alg.startsWith('HS')) return 'bg-blue-100 text-blue-700'
  if (alg.startsWith('RS')) return 'bg-purple-100 text-purple-700'
  if (alg.startsWith('ES')) return 'bg-emerald-100 text-emerald-700'
  if (alg.startsWith('PS')) return 'bg-orange-100 text-orange-700'
  return 'bg-muted text-foreground'
})

const jwtExpiry = computed(() => {
  if (!jwtResult.value) return null
  const payload = jwtResult.value.payload as Record<string, unknown>
  const exp = payload.exp as number | undefined
  if (exp === undefined) return null
  const now = Math.floor(Date.now() / 1000)
  const diff = exp - now
  const isExpired = diff < 0
  const abs = Math.abs(diff)
  const timeStr = abs < 60 ? `${abs}초` : abs < 3600 ? `${Math.floor(abs / 60)}분` : abs < 86400 ? `${Math.floor(abs / 3600)}시간` : `${Math.floor(abs / 86400)}일`
  return {isExpired, timeStr: isExpired ? `${timeStr} 전 만료` : `${timeStr} 후 만료`}
})

// ── 2-패널 도구 설정 ──────────────────────────────────────────────────────────
interface ToolConfig {
  inputLabel: string
  outputLabel: string
  placeholder: string
  example?: string
  hasToggle?: boolean
  toggleOptions?: { value: string; label: string }[]
  hasJsonToggle?: boolean
}

const CONFIGS: Record<string, ToolConfig> = {
  'json-formatter': {
    inputLabel: 'JSON', outputLabel: '결과',
    placeholder: '{"key": "value"}',
    example: '{"name":"홍길동","age":30,"skills":["Java","Vue","Spring"]}',
    hasJsonToggle: true,
  },
  'base64': {
    inputLabel: '텍스트', outputLabel: '결과',
    placeholder: '인코딩할 텍스트를 입력하세요',
    example: 'Hello, World! 안녕하세요.',
    hasToggle: true,
    toggleOptions: [{value: 'encode', label: '인코딩'}, {value: 'decode', label: '디코딩'}],
  },
  'url-encode': {
    inputLabel: '텍스트', outputLabel: '결과',
    placeholder: '인코딩할 텍스트를 입력하세요',
    example: 'https://example.com/search?q=한글 검색&page=1',
    hasToggle: true,
    toggleOptions: [{value: 'encode', label: '인코딩'}, {value: 'decode', label: '디코딩'}],
  },
  'char-count': {
    inputLabel: '텍스트', outputLabel: '통계',
    placeholder: '글자 수를 세고 싶은 텍스트를 입력하세요',
    example: 'The quick brown fox jumps over the lazy dog.\n빠른 갈색 여우가 게으른 개를 뛰어넘습니다.',
  },
  'keyboard-convert': {
    inputLabel: '텍스트', outputLabel: '변환 결과',
    placeholder: '변환할 텍스트를 입력하세요',
    example: 'gksrmf',
    hasToggle: true,
    toggleOptions: [{value: 'en-ko', label: '영→한'}, {value: 'ko-en', label: '한→영'}],
  },
  'whitespace': {
    inputLabel: '텍스트', outputLabel: '정규화 결과',
    placeholder: '공백을 정규화할 텍스트를 입력하세요',
    example: 'Hello   World\n\n\nThis   has   extra   spaces\tand\ttabs.',
  },
}

const config = computed<ToolConfig>(() => CONFIGS[props.moduleId] ?? {
  inputLabel: '입력', outputLabel: '결과', placeholder: '입력하세요',
})

const stats = computed(() => {
  if (props.moduleId !== 'char-count' || !input.value) return null
  const {chars, words, bytes} = countChars(input.value)
  return [
    {label: '문자', value: chars},
    {label: '단어', value: words},
    {label: '바이트', value: bytes},
  ]
})

function compute() {
  error.value = ''
  output.value = ''
  if (!input.value) return
  try {
    switch (props.moduleId) {
      case 'json-formatter':
        output.value = jsonMode.value === 'format'
            ? formatJson(input.value, jsonIndent.value, jsonSortKeys.value)
            : minifyJson(input.value, jsonSortKeys.value)
        break
      case 'base64':
        if (base64UrlSafe.value)
          output.value = direction.value === 'encode' ? encodeBase64Url(input.value) : decodeBase64Url(input.value)
        else
          output.value = direction.value === 'encode' ? encodeBase64(input.value) : decodeBase64(input.value)
        break
      case 'url-encode':
        output.value = direction.value === 'encode' ? encodeUrl(input.value) : decodeUrl(input.value)
        break
      case 'keyboard-convert':
        output.value = convertKeyboard(input.value, direction.value as 'ko-en' | 'en-ko')
        break
      case 'whitespace':
        output.value = normalizeWhitespace(input.value)
        break
      case 'char-count': {
        const {chars, words, bytes} = countChars(input.value)
        output.value = `${chars} 문자, ${words} 단어, ${bytes} 바이트`
        break
      }
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '변환 중 오류가 발생했습니다.'
  }
}

async function copyText(text: string) {
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}

const JWT_EXAMPLE = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c'

watch(() => props.moduleId, (id) => {
  input.value = ''
  output.value = ''
  error.value = ''
  uuidValue.value = ''
  uuidRawList.value = []
  uuidCount.value = 1
  uuidNoHyphen.value = false
  tsUnix.value = ''
  tsDate.value = ''
  tsError.value = ''
  colorInput.value = ''
  colorResult.value = null
  colorError.value = ''
  jwtInput.value = ''
  jwtResult.value = null
  jwtError.value = ''
  jwtActiveTab.value = 'payload'
  direction.value = CONFIGS[id]?.toggleOptions?.[0]?.value ?? 'encode'
  jsonMode.value = 'format'
  base64UrlSafe.value = false
  jsonIndent.value = 2
  jsonSortKeys.value = false
  urlMode.value = 'transform'
  urlQueryParams.value = []
  copied.value = false

  if (id === 'uuid') {
    generateUuids()
  } else if (id === 'timestamp') {
    tsUnix.value = '1700000000'
    onUnixInput()
  } else if (id === 'color-code') {
    colorInput.value = '#6366f1'
    computeColor()
  } else if (id === 'jwt-decoder') {
    jwtInput.value = JWT_EXAMPLE
    decodeJwtInput()
  } else {
    const example = CONFIGS[id]?.example
    if (example) {
      input.value = example;
      compute()
    }
  }
}, {immediate: true})
</script>
