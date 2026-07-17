<template>
  <CommandDialog v-model:open="isOpen">
    <CommandInput placeholder="도구 검색..." @input="onSearchInput"/>
    <CommandList class="max-h-[360px]">
      <CommandEmpty>도구를 찾을 수 없습니다.</CommandEmpty>
      <CommandGroup
          v-for="(group, groupLabel) in grouped"
          :key="groupLabel"
          :heading="String(groupLabel)"
      >
        <CommandItem
            v-for="mod in group"
            :key="mod.id"
            :value="`${mod.name} ${mod.category} ${mod.description ?? ''} ${keywordStrings(mod.keywords).join(' ')}`"
            class="flex items-center gap-2.5 py-2"
            @select="navigate(mod)"
        >
          <div class="flex size-6 shrink-0 items-center justify-center rounded bg-secondary text-muted-foreground">
            <component
                :is="getCategoryConfig(mod.category).icon"
                class="size-3.5"
            />
          </div>
          <span class="flex-1 truncate text-[13px]">{{ mod.name }}</span>
          <span class="shrink-0 font-mono text-[11px] text-muted-foreground">{{ mod.category }}</span>
        </CommandItem>
      </CommandGroup>
    </CommandList>
    <div class="flex items-center gap-5 border-t border-border px-4 py-2.5 font-mono text-[11px] text-muted-foreground">
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
import {zoneOf} from '../config/zones'
import {keywordStrings, resolveAliasQuery} from '../utils/keywordAlias'
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
const search = ref('')

function open() {
  search.value = ''
  isOpen.value = true
}

defineExpose({open})

function onSearchInput(e: Event) {
  search.value = (e.target as HTMLInputElement).value ?? ''
}

const grouped = computed(() =>
    props.modules.reduce<Record<string, Module[]>>((acc, mod) => {
      // zones[0]이 기본 구역 (ADR-0023) — 검색은 구역 무관 전체지만, 그룹 헤딩은 기본 구역으로 표시
      const groupLabel = `${zoneOf(mod.zones[0]).name} > ${mod.category}`
      ;(acc[groupLabel] ??= []).push(mod)
      return acc
    }, {}),
)

function navigate(mod: Module) {
  isOpen.value = false
  // 검색어가 딥링크 별칭과 일치하면 해당 모드/탭으로 바로 이동한다
  const query = resolveAliasQuery(mod.keywords, search.value)
  router.push(query ? `/tools/${mod.id}?${query}` : `/tools/${mod.id}`)
}
</script>
