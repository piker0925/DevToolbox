<script lang="ts" setup>
import type {InputGroupButtonProps} from "."
import {inputGroupButtonVariants} from "."
import {cn} from "@/lib/utils"
import {Button} from '@/components/ui/button'

const props = withDefaults(defineProps<InputGroupButtonProps>(), {
  size: "xs",
  variant: "ghost",
})
</script>

<template>
  <Button
      :class="cn(inputGroupButtonVariants({ size: props.size }), props.class)"
      :data-size="props.size"
      :variant="props.variant"
      type="button"
  >
    <slot/>
  </Button>
</template>
