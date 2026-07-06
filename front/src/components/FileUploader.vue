<template>
  <div
      :class="{ dragging }"
      class="file-uploader"
      @click="fileInput?.click()"
      @dragleave="dragging = false"
      @dragover.prevent="dragging = true"
      @drop.prevent="onDrop"
  >
    <input ref="fileInput" hidden multiple type="file" @change="onChange"/>
    <slot>
      <div style="font-size:2rem;margin-bottom:.5rem">📂</div>
      <p>파일을 드래그하거나 클릭하여 선택하세요</p>
      <p style="font-size:.75rem;margin-top:.25rem;opacity:.7">여러 파일 동시 업로드 가능</p>
    </slot>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {apiClient} from '../api/client'
import type {UploadResult} from '../types'

const props = defineProps<{ moduleId: string }>()
const emit = defineEmits<{
  uploaded: [result: UploadResult]
}>()

const dragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

async function upload(files: File[]) {
  const form = new FormData()
  form.append('moduleId', props.moduleId)
  files.forEach(f => form.append('files', f))
  const {data} = await apiClient.post<UploadResult>('/api/v1/upload', form)
  emit('uploaded', data)
}

function onDrop(e: DragEvent) {
  dragging.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  if (files.length) upload(files)
}

function onChange(e: Event) {
  const files = Array.from((e.target as HTMLInputElement).files ?? [])
  if (files.length) upload(files)
}
</script>
