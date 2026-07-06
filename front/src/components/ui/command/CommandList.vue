<script lang="ts" setup>
import type {ListboxContentProps} from "reka-ui"
import {ListboxContent, useForwardProps} from "reka-ui"
import type {HTMLAttributes} from "vue"
import {reactiveOmit} from "@vueuse/core"
import {cn} from "@/lib/utils"

const props = defineProps<ListboxContentProps & { class?: HTMLAttributes["class"] }>()

const delegatedProps = reactiveOmit(props, "class")

const forwarded = useForwardProps(delegatedProps)
</script>

<template>
  <ListboxContent
      :class="cn('no-scrollbar max-h-72 scroll-py-1 outline-none overflow-x-hidden overflow-y-auto', props.class)"
      data-slot="command-list"
      v-bind="forwarded"
  >
    <div role="presentation">
      <slot/>
    </div>
  </ListboxContent>
</template>
