<template>
  <div class="flex h-screen overflow-hidden">

    <!-- 모바일 드로어 백드롭 -->
    <div
        v-if="drawerOpen"
        class="fixed inset-0 z-30 bg-black/50 lg:hidden"
        @click="drawerOpen = false"
    />

    <aside
        :class="drawerOpen ? 'translate-x-0' : '-translate-x-full'"
        class="fixed inset-y-0 left-0 z-40 flex w-[280px] shrink-0 flex-col bg-sidebar transition-transform duration-200 lg:static lg:translate-x-0"
    >

      <!-- Logo -->
      <button
          class="flex items-center gap-2.5 px-4 py-4 transition-opacity hover:opacity-80"
          @click="handleCategoryClick(null)"
      >
        <div class="flex size-7 items-center justify-center rounded-lg bg-sidebar-primary">
          <Zap class="size-4 text-sidebar-primary-foreground"/>
        </div>
        <span class="text-[15px] font-semibold text-white">DevToolbox</span>
      </button>

      <!-- Search -->
      <div class="px-3 pb-3">
        <button
            class="flex w-full items-center gap-2 rounded-lg border border-sidebar-border bg-white/6 px-3 py-2 text-left transition-colors hover:border-white/15 hover:bg-white/10"
            @click="paletteRef?.open()"
        >
          <Search class="size-[14px] shrink-0 text-sidebar-foreground/60"/>
          <span class="flex-1 text-[13px] text-sidebar-foreground/60">검색...</span>
          <kbd class="shrink-0 font-mono text-[11px] text-sidebar-foreground/40">{{ shortcutKey }}</kbd>
        </button>
      </div>

      <!-- Nav -->
      <nav class="flex-1 overflow-y-auto px-2 pb-3">

        <!-- Favorites -->
        <template v-if="favoriteModules.length > 0">
          <div class="mb-1 px-3">
            <span
                class="font-mono text-[11px] font-medium uppercase tracking-wider text-sidebar-foreground/45">즐겨찾기</span>
          </div>
          <button
              v-for="mod in favoriteModules"
              :key="mod.id"
              class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              @click="router.push(`/tools/${mod.id}`)"
          >
            <Star class="size-[13px] shrink-0 fill-amber-400/80 text-amber-400/80"/>
            <span class="flex-1 truncate text-[13px]">{{ mod.name }}</span>
          </button>
          <div class="mx-1 my-2 border-t border-sidebar-border"/>
        </template>

        <!-- 전체 -->
        <button
            :class="isAllActive
            ? 'bg-white/10 text-white'
            : 'text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'"
            class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left transition-colors"
            @click="handleCategoryClick(null)"
        >
          <LayoutGrid class="size-[16px] shrink-0"/>
          <span class="flex-1 text-[14px] font-medium">전체 도구</span>
          <span class="shrink-0 font-mono text-[11px] opacity-40">{{ modules.length }}</span>
        </button>

        <div class="mx-1 my-2 border-t border-sidebar-border"/>

        <!-- Categories -->
        <button
            v-for="cat in CATEGORIES"
            :key="cat.name"
            :class="isCategoryActive(cat.name)
            ? 'bg-white/10 text-white'
            : 'text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'"
            class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left transition-colors"
            @click="handleCategoryClick(cat.name)"
        >
          <component :is="cat.icon" class="size-[16px] shrink-0"/>
          <span class="flex-1 text-[14px]">{{ cat.name }}</span>
          <span class="shrink-0 font-mono text-[11px] opacity-40">{{ categoryCounts[cat.name] }}</span>
        </button>
      </nav>

      <!-- 하단: 테마 · 건의하기 -->
      <div class="flex items-center gap-1 border-t border-sidebar-border p-2">
        <router-link
            class="flex flex-1 items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            to="/suggestions"
        >
          <MessageSquarePlus class="size-[15px] shrink-0"/>
          <span>건의하기</span>
        </router-link>
        <button
            :title="themeLabel"
            class="flex size-9 shrink-0 items-center justify-center rounded-lg text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            @click="cycleTheme"
        >
          <Sun v-if="preference === 'light'" class="size-[15px]"/>
          <Moon v-else-if="preference === 'dark'" class="size-[15px]"/>
          <MonitorSmartphone v-else class="size-[15px]"/>
        </button>
      </div>
    </aside>

    <!-- Main -->
    <div class="flex min-w-0 flex-1 flex-col">
      <!-- 모바일 톱바 -->
      <header class="flex h-12 shrink-0 items-center gap-1 border-b border-border bg-background px-2 lg:hidden">
        <button
            class="flex size-9 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
            title="메뉴 열기"
            @click="drawerOpen = true"
        >
          <Menu class="size-[18px]"/>
        </button>
        <button class="flex items-center gap-2 px-1" @click="handleCategoryClick(null)">
          <div class="flex size-6 items-center justify-center rounded-md bg-primary">
            <Zap class="size-3.5 text-primary-foreground"/>
          </div>
          <span class="text-[14px] font-semibold text-foreground">DevToolbox</span>
        </button>
        <button
            class="ml-auto flex size-9 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
            title="검색"
            @click="paletteRef?.open()"
        >
          <Search class="size-[16px]"/>
        </button>
      </header>

      <main class="flex-1 overflow-y-auto bg-background">
        <router-view/>
      </main>
    </div>

    <CommandPalette ref="paletteRef" :modules="modules"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {LayoutGrid, Menu, MessageSquarePlus, MonitorSmartphone, Moon, Search, Star, Sun, Zap} from 'lucide-vue-next'
import {apiClient} from '../api/client'
import {MOCK_MODULES} from '../api/mock'
import {normalizeApiModules} from '../api/modules'
import type {Module} from '../types'
import {CATEGORY_CONFIG, CATEGORY_ORDER} from '../utils/categoryConfig'
import {useToolFilter} from '../composables/useToolFilter'
import {useFavorites} from '../composables/useFavorites'
import {useTheme} from '../composables/useTheme'
import CommandPalette from '../components/CommandPalette.vue'

const route = useRoute()
const router = useRouter()
const modules = ref<Module[]>([])
const paletteRef = ref<InstanceType<typeof CommandPalette> | null>(null)
const drawerOpen = ref(false)

const {activeCategory, setCategory} = useToolFilter()
const {favoriteIds} = useFavorites()
const {preference, setTheme} = useTheme()

const shortcutKey = navigator.userAgent.includes('Mac') ? '⌘K' : 'Ctrl K'

const THEME_CYCLE = {light: 'dark', dark: 'system', system: 'light'} as const
const THEME_LABEL = {light: '라이트 테마', dark: '다크 테마', system: '시스템 테마'} as const

const themeLabel = computed(() => `${THEME_LABEL[preference.value]} (클릭하여 전환)`)

function cycleTheme() {
  setTheme(THEME_CYCLE[preference.value])
}

const favoriteModules = computed(() =>
    favoriteIds.value
        .map(id => modules.value.find(m => m.id === id))
        .filter((m): m is Module => m !== undefined),
)

const CATEGORIES = CATEGORY_ORDER.map(name => ({
  name,
  icon: CATEGORY_CONFIG[name]?.icon,
}))

const categoryCounts = computed(() =>
    modules.value.reduce<Record<string, number>>((acc, mod) => {
      acc[mod.category] = (acc[mod.category] ?? 0) + 1
      return acc
    }, {}),
)

const isAllActive = computed(() =>
    route.path === '/' && activeCategory.value === null,
)

// 모바일 드로어는 페이지 이동 시 닫는다
watch(() => route.fullPath, () => {
  drawerOpen.value = false
})

function isCategoryActive(catName: string) {
  if (route.path === '/') return activeCategory.value === catName
  const moduleId = route.params.moduleId as string | undefined
  if (!moduleId) return false
  return modules.value.find(m => m.id === moduleId)?.category === catName
}

function handleCategoryClick(catName: string | null) {
  setCategory(catName)
  drawerOpen.value = false
  if (route.path !== '/') router.push('/')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    paletteRef.value?.open()
  }
  if (e.key === 'Escape' && drawerOpen.value) {
    drawerOpen.value = false
  }
}

onMounted(() => {
  loadModules()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

async function loadModules() {
  try {
    const {data} = await apiClient.get<Module[]>('/api/v1/modules')
    modules.value = normalizeApiModules(data)
  } catch {
    modules.value = MOCK_MODULES
  }
}
</script>
