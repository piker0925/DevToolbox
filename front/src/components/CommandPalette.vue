<template>
  <CommandDialog v-model:open="isOpen">
    <CommandInput placeholder="도구 검색..."/>
    <CommandList class="max-h-[360px]">
      <CommandEmpty>도구를 찾을 수 없습니다.</CommandEmpty>
      <CommandGroup
          v-for="(group, category) in grouped"
          :key="category"
          :heading="String(category)"
      >
        <CommandItem
            v-for="mod in group"
            :key="mod.id"
            :value="`${mod.name} ${mod.category} ${mod.description ?? ''}`"
            class="flex items-center gap-2.5 py-2"
            @select="navigate(mod.id)"
        >
          <div
              :class="getCategoryConfig(String(category)).bg"
              class="flex size-6 shrink-0 items-center justify-center rounded"
          >
            <component
                :is="getCategoryConfig(String(category)).icon"
                :class="getCategoryConfig(String(category)).color"
                class="size-3.5"
            />
          </div>
          <span class="flex-1 truncate text-[13px]">{{ mod.name }}</span>
          <span
              :class="getCategoryConfig(String(category)).color"
              class="shrink-0 text-[11px]"
          >{{ category }}</span>
        </CommandItem>
      </CommandGroup>
    </CommandList>
    <div class="flex items-center gap-5 border-t border-slate-100 px-4 py-2.5 text-[11px] text-slate-400">
      <span>↑↓ 이동</span>
      <span>↵ 선택</span>
      <span>Esc 닫기</span>
    </div>
  </CommandDialog>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import type {Module} from '../types'
import {getCategoryConfig} from '../utils/categoryConfig'
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'

const props = defineProps<{ modules: Module[] }>()
const router = useRouter()
const isOpen = ref(false)

function open() {
  isOpen.value = true
}

defineExpose({open})

const grouped = computed(() =>
    props.modules.reduce<Record<string, Module[]>>((acc, mod) => {
      ;(acc[mod.category] ??= []).push(mod)
      return acc
    }, {}),
)

function navigate(moduleId: string) {
  isOpen.value = false
  router.push(`/tools/${moduleId}`)
}
</script>
