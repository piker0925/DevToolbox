<template>
  <div class="flex h-full flex-col gap-3">
    <template v-if="url">
      <img
          v-if="isImage"
          :src="url"
          alt="처리 결과 미리보기"
          class="max-h-[360px] w-fit rounded-md border border-border object-contain"
      />
      <!-- 액션 행: 다운로드(주) + 보조 액션 슬롯. 가로 한 줄로 묶어 폭이 늘어나지 않게 한다. -->
      <div class="flex flex-wrap items-center gap-2">
        <Button as-child>
          <a :href="url" download>⬇ 다운로드</a>
        </Button>
        <slot name="actions"/>
      </div>
      <!-- 파일 결과에 동반되는 advisory (예: 업스케일 경고) -->
      <p v-if="text" class="text-[12px] text-amber-600 dark:text-amber-400">{{ text }}</p>
    </template>
    <template v-else-if="text">
      <Textarea :model-value="text" class="min-h-[240px] flex-1 resize-y font-mono text-sm" readonly/>
      <div class="flex flex-wrap items-center gap-2">
        <Button variant="outline" @click="copy">복사</Button>
        <slot name="actions"/>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {Button} from '@/components/ui/button'
import {Textarea} from '@/components/ui/textarea'

const props = defineProps<{ url: string | null; text: string | null }>()

const isImage = computed(() => !!props.url && /\.(png|jpe?g|gif|webp)$/i.test(props.url))

function copy() {
  if (props.text) navigator.clipboard.writeText(props.text)
}
</script>
