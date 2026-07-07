<template>
  <div class="mt-8 border-t pt-6">
    <h3 class="mb-4 text-sm font-semibold text-zinc-700">댓글</h3>

    <!-- Loading -->
    <div v-if="loading" class="mb-4 py-4 text-center text-sm text-zinc-400">불러오는 중...</div>

    <!-- Empty -->
    <div v-else-if="comments.length === 0" class="mb-4 py-8 text-center">
      <MessageSquare class="mx-auto mb-2 size-7 text-zinc-200"/>
      <p class="text-sm text-zinc-400">아직 댓글이 없습니다.</p>
    </div>

    <!-- Comments list -->
    <ul v-else class="mb-4 space-y-3">
      <li v-for="c in comments" :key="c.id" class="rounded-md border border-zinc-100 bg-zinc-50 px-4 py-3">
        <p class="text-sm text-zinc-800">{{ c.content }}</p>
        <p class="mt-1 text-xs text-zinc-400">{{ formatDate(c.createdAt) }}</p>
      </li>
    </ul>

    <!-- New comment form -->
    <div class="space-y-2">
      <Textarea
          v-model="newContent"
          class="min-h-[72px] resize-none text-sm"
          placeholder="댓글을 남겨주세요..."
      />
      <div class="flex items-center justify-between">
        <p class="text-xs text-zinc-400">익명 · 로그인 없이 댓글 작성</p>
        <Button
            :disabled="submitting || !newContent.trim()"
            class="text-xs"
            size="sm"
            variant="outline"
            @click="submitComment"
        >{{ submitting ? '등록 중...' : '댓글 등록' }}</Button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {MessageSquare} from 'lucide-vue-next'
import {Button} from '@/components/ui/button'
import {Textarea} from '@/components/ui/textarea'
import {apiClient} from '@/api/client'

interface Comment {
  id: number
  content: string
  createdAt: string
}

const props = defineProps<{ moduleId: string }>()

const comments = ref<Comment[]>([])
const loading = ref(false)
const submitting = ref(false)
const newContent = ref('')

async function loadComments() {
  loading.value = true
  try {
    const {data} = await apiClient.get<Comment[]>(`/api/v1/tools/${props.moduleId}/comments`)
    comments.value = data
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!newContent.value.trim()) return
  submitting.value = true
  try {
    await apiClient.post(`/api/v1/tools/${props.moduleId}/comments`, {content: newContent.value})
    newContent.value = ''
    await loadComments()
  } catch {
    // 제출 실패 시 조용히 무시
  } finally {
    submitting.value = false
  }
}

function formatDate(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

onMounted(loadComments)
</script>
