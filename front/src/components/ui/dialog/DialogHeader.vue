<script lang="ts" setup>
import type {HTMLAttributes} from "vue"
import {cn} from "@/lib/utils"

const props = defineProps<{
  class?: HTMLAttributes["class"]
}>()
</script>

<template>
  <div
      :class="cn('gap-2 flex flex-col', props.class)"
      data-slot="dialog-header"
  >
    <slot/>
  </div>
</template>
