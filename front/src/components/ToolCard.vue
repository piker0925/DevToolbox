<template>
  <div
      :title="mod.description ?? mod.name"
      :class="!mod.implemented ? 'opacity-50' : ''"
      class="group relative flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3.5 shadow-sm transition-all duration-150 hover:border-slate-300 hover:bg-slate-50 hover:shadow"
      @click="router.push(`/tools/${mod.id}`)"
  >
    <!-- WIP badge (개발 중 표시) -->
    <span
        v-if="!mod.implemented"
        class="absolute right-2 top-2 rounded px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wide bg-slate-100 text-slate-400"
    >WIP</span>

    <!-- Category icon -->
    <div
        :class="config.thumbBg"
        class="flex size-9 shrink-0 items-center justify-center rounded-lg"
    >
      <component :is="config.icon" class="size-4 text-white"/>
    </div>

    <!-- Name + Description -->
    <div class="min-w-0 flex-1">
      <p class="truncate text-[14px] font-semibold text-slate-800 group-hover:text-indigo-700">
        {{ mod.name }}
      </p>
      <p v-if="mod.description" class="truncate text-[12px] text-slate-400">
        {{ mod.description }}
      </p>
    </div>

    <!-- Favorite -->
    <button
        :class="isFav
        ? 'opacity-100 text-amber-400'
        : 'opacity-0 group-hover:opacity-100 text-slate-300 hover:text-amber-400'"
        :title="isFav ? '즐겨찾기 해제' : '즐겨찾기 추가'"
        class="flex size-6 shrink-0 items-center justify-center rounded transition-all"
        @click.stop="toggle(mod.id)"
    >
      <Star :class="isFav ? 'fill-amber-400' : ''" class="size-[13px]"/>
    </button>

    <!-- Arrow -->
    <ChevronRight class="size-4 shrink-0 text-slate-300 transition-colors group-hover:text-slate-400"/>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {useRouter} from 'vue-router'
import {ChevronRight, Star} from 'lucide-vue-next'
import {getCategoryConfig} from '../utils/categoryConfig'
import {useFavorites} from '../composables/useFavorites'
import type {Module} from '../types'

const props = defineProps<{ mod: Module }>()
const router = useRouter()
const config = computed(() => getCategoryConfig(props.mod.category))

const {isFavorite, toggle} = useFavorites()
const isFav = computed(() => isFavorite(props.mod.id))
</script>
