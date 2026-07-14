<template>
  <div class="flex h-full flex-col gap-3">
    <template v-if="text">
      <Textarea :model-value="text" class="min-h-[240px] flex-1 resize-y font-mono text-sm" readonly/>
      <Button variant="outline" class="w-fit" @click="copy">복사</Button>
    </template>
  </div>
</template>

<script lang="ts" setup>
// 텍스트 결과 전용 뷰어. 파일(이미지·PDF·ZIP) 결과는 FileResultPanel이 담당한다.
import {Button} from '@/components/ui/button'
import {Textarea} from '@/components/ui/textarea'

const props = defineProps<{ text: string | null }>()

function copy() {
  if (props.text) navigator.clipboard.writeText(props.text)
}
</script>
