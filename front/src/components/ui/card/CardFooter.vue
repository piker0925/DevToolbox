<script lang="ts" setup>
import type {HTMLAttributes} from "vue"
import {cn} from "@/lib/utils"

const props = defineProps<{
  class?: HTMLAttributes["class"]
}>()
</script>

<template>
  <div
      :class="cn('bg-muted/50 rounded-b-xl border-t p-4 group-data-[size=sm]/card:p-3 flex items-center', props.class)"
      data-slot="card-footer"
  >
    <slot/>
  </div>
</template>
