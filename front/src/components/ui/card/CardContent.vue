<script lang="ts" setup>
import type {HTMLAttributes} from "vue"
import {cn} from "@/lib/utils"

const props = defineProps<{
  class?: HTMLAttributes["class"]
}>()
</script>

<template>
  <div
      :class="cn('px-4 group-data-[size=sm]/card:px-3', props.class)"
      data-slot="card-content"
  >
    <slot/>
  </div>
</template>
