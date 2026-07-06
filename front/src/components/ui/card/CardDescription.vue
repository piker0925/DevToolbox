<script lang="ts" setup>
import type {HTMLAttributes} from "vue"
import {cn} from "@/lib/utils"

const props = defineProps<{
  class?: HTMLAttributes["class"]
}>()
</script>

<template>
  <div
      :class="cn('text-muted-foreground text-sm', props.class)"
      data-slot="card-description"
  >
    <slot/>
  </div>
</template>
