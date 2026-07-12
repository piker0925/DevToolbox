<template>
  <slot/>
</template>

<script lang="ts" setup>
import {onMounted, onUnmounted} from 'vue'
import {apiClient} from '../api/client'
import type {BatchProgress} from '../types'

const props = defineProps<{ batchId: string; intervalMs?: number }>()
const emit = defineEmits<{
  progress: [progress: BatchProgress]
  done: [progress: BatchProgress]
}>()

let timerId: ReturnType<typeof setTimeout> | null = null

function isComplete(p: BatchProgress): boolean {
  // total이 0이면 아직 job이 잡히지 않은 상태 — 완료로 오판하지 않는다.
  return p.totalCount > 0 && p.doneCount + p.failCount >= p.totalCount
}

async function poll() {
  const {data} = await apiClient.get<BatchProgress>(`/api/v1/batches/${props.batchId}`)
  if (isComplete(data)) {
    emit('done', data)
    return
  }
  emit('progress', data)
  timerId = setTimeout(poll, props.intervalMs ?? 2000)
}

onMounted(poll)
onUnmounted(() => {
  if (timerId) clearTimeout(timerId)
})
</script>
