<template>
  <slot/>
</template>

<script lang="ts" setup>
import {onMounted, onUnmounted} from 'vue'
import {apiClient} from '../api/client'
import type {Job} from '../types'

const props = defineProps<{ jobId: string; intervalMs?: number }>()
const emit = defineEmits<{
  done: [job: Job]
  failed: [job: Job]
}>()

let timerId: ReturnType<typeof setTimeout> | null = null

async function poll() {
  const {data} = await apiClient.get<Job>(`/api/v1/jobs/${props.jobId}`)
  if (data.status === 'DONE') {
    emit('done', data)
    return
  }
  if (data.status === 'FAILED') {
    emit('failed', data)
    return
  }
  timerId = setTimeout(poll, props.intervalMs ?? 3000)
}

onMounted(poll)
onUnmounted(() => {
  if (timerId) clearTimeout(timerId)
})
</script>
