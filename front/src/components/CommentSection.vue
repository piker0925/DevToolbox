<template>
  <div class="pt-4">
    <!-- Loading -->
    <div v-if="loading" class="mb-4 py-4 text-center text-sm text-muted-foreground">불러오는 중...</div>

    <!-- Empty -->
    <div v-else-if="comments.length === 0" class="mb-4 py-6 text-center">
      <p class="text-sm text-muted-foreground">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</p>
    </div>

    <!-- Comments list -->
    <ul v-else class="mb-4 space-y-3">
      <li v-for="c in comments" :key="c.id" class="rounded-md border border-border bg-muted/40 px-4 py-3 relative group">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-semibold" :class="c.nickname ? 'text-foreground' : 'text-muted-foreground'">
            {{ c.nickname || '익명' }}
          </span>
          <div class="flex items-center space-x-2">
            <span class="font-mono text-xs text-muted-foreground">{{ formatDate(c.createdAt) }}</span>
            <button 
              v-if="isLoggedIn && user?.nickname === c.nickname && c.nickname !== null" 
              @click="deleteComment(c.id)"
              class="text-xs text-destructive opacity-0 group-hover:opacity-100 transition-opacity hover:underline"
              title="댓글 삭제"
            >
              삭제
            </button>
          </div>
        </div>
        <p class="text-sm text-foreground whitespace-pre-wrap">{{ c.content }}</p>
      </li>
    </ul>

    <!-- New comment form -->
    <div class="space-y-2">
      <Textarea
          v-model="newContent"
          class="min-h-[72px] resize-none text-sm"
          placeholder="댓글을 남겨주세요... (Ctrl + Enter로 등록)"
          @keydown.ctrl.enter.prevent="handleShortcut"
          @keydown.meta.enter.prevent="handleShortcut"
      />
      <div class="flex items-center justify-between">
        <p class="text-[11px]" :class="isLoggedIn ? 'text-foreground font-medium' : 'text-muted-foreground'">
          <template v-if="isLoggedIn">{{ user?.nickname }} (으)로 댓글 작성</template>
          <template v-else>익명 · 로그인 없이 작성</template>
        </p>
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
import {Button} from '@/components/ui/button'
import {Textarea} from '@/components/ui/textarea'
import {apiClient} from '@/api/client'
import {useAuth} from '@/composables/useAuth'

interface Comment {
  id: number
  content: string
  createdAt: string
  nickname?: string | null
}

const props = defineProps<{ moduleId: string }>()
const emit = defineEmits<{ count: [count: number] }>()

const { isLoggedIn, user } = useAuth()

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
    emit('count', comments.value.length)
  }
}

function handleShortcut(e: KeyboardEvent) {
  // 한글 입력 중(IME 조합 중) 단축키를 누르면 Vue v-model이 마지막 글자를 미처 동기화하지 못함.
  // 이 때 DOM의 실제 value를 강제로 끌어와서 동기화한 뒤 제출합니다.
  const target = e.target as HTMLTextAreaElement
  if (target) {
    newContent.value = target.value
  }
  submitComment()
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

async function deleteComment(id: number) {
  if (!confirm('댓글을 삭제하시겠습니까?')) return
  try {
    await apiClient.delete(`/api/v1/comments/${id}`)
    await loadComments()
  } catch (e) {
    console.error('Failed to delete comment', e)
    alert('댓글 삭제에 실패했습니다.')
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
