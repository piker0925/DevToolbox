<template>
  <pre class="h-full overflow-auto whitespace-pre-wrap break-all p-4 font-mono text-[13px] text-foreground"><code v-html="html"></code></pre>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {ensureHighlightStyles, highlightToHtml} from '../utils/highlight'

const props = defineProps<{ code: string; language?: string }>()

ensureHighlightStyles()

// highlightToHtml은 미등록 언어/실패 시 HTML 이스케이프된 원문을 반환하므로 v-html에 안전하다.
const html = computed(() => highlightToHtml(props.code, props.language))
</script>
