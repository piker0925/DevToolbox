<template>
  <div class="mx-auto flex w-full max-w-[1200px] flex-col px-4 pb-10 sm:px-6">

    <!-- Page title -->
    <div class="pb-2 pt-8">
      <h1 class="text-xl font-semibold tracking-tight text-foreground">
        {{ activeCategory ?? zone.name }}
        <span class="ml-1.5 align-middle font-mono text-[13px] font-normal text-muted-foreground">{{
            filteredModules.length
          }}</span>
      </h1>
      <p class="mt-1 text-[13px] text-muted-foreground">
        {{ activeCategory ? `${activeCategory} 카테고리의 도구입니다.` : zone.description }}
      </p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="grid grid-cols-1 gap-3 pt-4 sm:grid-cols-2 xl:grid-cols-3">
      <div v-for="n in 18" :key="n" class="h-[68px] animate-pulse rounded-xl bg-muted"/>
    </div>

    <!-- 빈 구역 안내 (즐겨찾기·최근 사용도 없을 때만) -->
    <div
        v-else-if="activeCategory === null && zoneModules.length === 0 && favoriteModules.length === 0 && recentModules.length === 0"
        class="pt-10 text-center"
    >
      <p class="text-[14px] text-muted-foreground">{{ zone.name }} 구역은 준비 중입니다. 곧 도구가 추가됩니다.</p>
    </div>

    <template v-else-if="activeCategory === null">
      <!-- 즐겨찾기 -->
      <section v-if="favoriteModules.length > 0">
        <h2 class="flex items-baseline gap-2 pb-2 pt-6">
          <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">즐겨찾기</span>
        </h2>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <ToolCard v-for="mod in favoriteModules" :key="mod.id" :mod="mod"/>
        </div>
      </section>

      <!-- 최근 사용 -->
      <section v-if="recentModules.length > 0">
        <h2 class="flex items-baseline gap-2 pb-2 pt-6">
          <span class="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">최근 사용</span>
        </h2>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <ToolCard v-for="mod in recentModules" :key="mod.id" :mod="mod"/>
        </div>
      </section>

      <!-- 카테고리별 섹션 -->
      <section v-for="section in categorySections" :key="section.name">
        <h2 class="flex items-baseline gap-2 pb-2 pt-6">
          <span class="text-[13px] font-semibold text-foreground">{{ section.name }}</span>
          <span class="font-mono text-[11px] text-muted-foreground">{{ section.modules.length }}</span>
        </h2>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <ToolCard
              v-for="(mod, i) in section.modules"
              :key="mod.id"
              :mod="mod"
              :style="{'--stagger-i': Math.min(section.offset + i, 24)}"
              class="stagger-in"
          />
        </div>
      </section>
    </template>

    <!-- 카테고리 필터 시 단일 그리드 -->
    <div v-else class="grid grid-cols-1 gap-3 pt-4 sm:grid-cols-2 xl:grid-cols-3">
      <ToolCard
          v-for="(mod, i) in filteredModules"
          :key="mod.id"
          :mod="mod"
          :style="{'--stagger-i': Math.min(i, 24)}"
          class="stagger-in"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {apiClient} from '../api/client'
import {MOCK_MODULES} from '../api/mock'
import {normalizeApiModules} from '../api/modules'
import type {Module} from '../types'
import {CATEGORY_ORDER} from '../utils/categoryConfig'
import {ZONES, type ZoneId} from '../config/zones'
import {useToolFilter} from '../composables/useToolFilter'
import {useFavorites} from '../composables/useFavorites'
import {useRecentTools} from '../composables/useRecentTools'
import ToolCard from '../components/ToolCard.vue'

const props = defineProps<{ zoneId: ZoneId }>()

const zone = computed(() => ZONES.find(z => z.id === props.zoneId)!)

const {activeCategory} = useToolFilter()
const {favoriteIds} = useFavorites()
const {recentIds} = useRecentTools()
const modules = ref<Module[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const {data} = await apiClient.get<Module[]>('/api/v1/modules')
    modules.value = normalizeApiModules(data)
  } catch {
    modules.value = MOCK_MODULES
  } finally {
    loading.value = false
  }
})

const zoneModules = computed(() => modules.value.filter(m => m.zones.includes(props.zoneId)))

const filteredModules = computed(() =>
    activeCategory.value
        ? zoneModules.value.filter(m => m.category === activeCategory.value)
        : zoneModules.value,
)

const favoriteModules = computed(() =>
    favoriteIds.value
        .map(id => modules.value.find(m => m.id === id))
        .filter((m): m is Module => m !== undefined),
)

const recentModules = computed(() =>
    recentIds.value
        .filter(id => !favoriteIds.value.includes(id))
        .map(id => modules.value.find(m => m.id === id))
        .filter((m): m is Module => m !== undefined),
)

const categorySections = computed(() => {
  let offset = 0
  return CATEGORY_ORDER
      .map(name => {
        const mods = zoneModules.value.filter(m => m.category === name)
        const section = {name, modules: mods, offset}
        offset += mods.length
        return section
      })
      .filter(s => s.modules.length > 0)
})
</script>
