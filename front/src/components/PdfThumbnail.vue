<template>
  <div class="flex h-full w-full items-center justify-center">
    <!-- 렌더 실패: 조용히 다운로드만 두는 대신, 미리보기를 못 불러왔음을 알린다. -->
    <p v-if="status === 'error'" class="text-[12px] text-muted-foreground">
      ⚠ 미리보기를 불러오지 못했습니다
    </p>
    <canvas
        v-else
        ref="canvasEl"
        class="max-h-full w-auto max-w-full rounded-md border border-border bg-white object-contain"
        :class="{ 'opacity-0': status === 'loading' }"
    />
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'

const props = defineProps<{ url: string }>()
const emit = defineEmits<{ pages: [count: number] }>()

// 썸네일이 아니라 패널을 채우는 미리보기이므로 렌더 해상도를 넉넉히 잡고(크기·선명도),
// 실제 표시 크기는 CSS(max-h/max-w)로 패널에 맞춰 줄인다.
const RENDER_MAX_WIDTH = 600

type Status = 'loading' | 'done' | 'error'
const status = ref<Status>('loading')
const canvasEl = ref<HTMLCanvasElement | null>(null)

async function render(url: string) {
  status.value = 'loading'
  try {
    // pdfjs와 워커는 PDF 결과가 있을 때만 지연 로딩한다(초기 번들에 포함 안 됨).
    const pdfjs = await import('pdfjs-dist')
    pdfjs.GlobalWorkerOptions.workerSrc =
        (await import('pdfjs-dist/build/pdf.worker.min.mjs?url')).default

    const doc = await pdfjs.getDocument({url}).promise
    emit('pages', doc.numPages)

    const page = await doc.getPage(1)
    const base = page.getViewport({scale: 1})
    const scale = Math.min(2, RENDER_MAX_WIDTH / base.width)
    const viewport = page.getViewport({scale})

    const canvas = canvasEl.value
    if (!canvas) throw new Error('canvas not ready')
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('2d context unavailable')
    canvas.width = viewport.width
    canvas.height = viewport.height
    await page.render({canvas, canvasContext: ctx, viewport}).promise

    status.value = 'done'
  } catch {
    status.value = 'error'
  }
}

onMounted(() => render(props.url))
watch(() => props.url, (url) => render(url))
</script>
