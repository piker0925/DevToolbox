<template>
  <div class="mt-8 border-t pt-6">
    <h3 class="mb-4 text-sm font-semibold text-zinc-700">댓글</h3>

    <div class="mb-4 py-8 text-center">
      <MessageSquare class="mx-auto mb-2 size-7 text-zinc-200"/>
      <p class="text-sm text-zinc-400">아직 댓글이 없습니다.</p>
    </div>

    <div class="space-y-2">
      <Textarea
          class="min-h-[72px] resize-none text-sm"
          disabled
          placeholder="댓글을 남겨주세요..."
      />
      <div class="flex items-center justify-between">
        <p class="text-xs text-zinc-400">익명 · 로그인 없이 댓글 작성</p>
        <Button class="text-xs" disabled size="sm" variant="outline">준비 중</Button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {MessageSquare} from 'lucide-vue-next'
import {Button} from '@/components/ui/button'
import {Textarea} from '@/components/ui/textarea'

defineProps<{ moduleId: string }>()
</script>
