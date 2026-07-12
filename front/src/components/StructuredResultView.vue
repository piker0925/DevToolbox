<template>
  <!-- table -->
  <div v-if="isTable(data)" class="overflow-x-auto">
    <table class="w-full text-[12px]">
      <thead>
      <tr class="border-b border-border">
        <th
            v-for="col in data.columns"
            :key="col"
            class="px-3 py-2 text-left font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground"
        >{{ col }}
        </th>
        <th class="w-8"/>
      </tr>
      </thead>
      <tbody>
      <tr v-for="(row, i) in data.rows" :key="i" class="border-b border-border/50 last:border-0 hover:bg-muted/30">
        <td v-for="(cell, j) in row" :key="j" class="break-all px-3 py-2 font-mono text-foreground">{{ cell }}</td>
        <td class="px-1 text-right">
          <button
              :class="copiedRow === i ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
              class="rounded p-0.5 transition-colors"
              title="행 복사"
              @click="copyRow(row.join('\t'), i)"
          >
            <Check v-if="copiedRow === i" class="size-3"/>
            <Copy v-else class="size-3"/>
          </button>
        </td>
      </tr>
      </tbody>
    </table>
  </div>

  <!-- keyvalue -->
  <div v-else-if="isKeyValue(data)" class="flex flex-col divide-y divide-border/50">
    <div v-for="(item, i) in data.items" :key="i" class="flex items-start gap-3 px-3 py-2 hover:bg-muted/30">
      <span class="w-36 shrink-0 pt-0.5 font-mono text-[11px] font-medium text-muted-foreground">{{ item.key }}</span>
      <span class="flex-1 break-all font-mono text-[12px] text-foreground">{{ item.value }}</span>
      <button
          :class="copiedRow === i ? 'text-emerald-500' : 'text-muted-foreground/50 hover:text-foreground'"
          class="shrink-0 rounded p-0.5 transition-colors"
          title="값 복사"
          @click="copyRow(item.value, i)"
      >
        <Check v-if="copiedRow === i" class="size-3"/>
        <Copy v-else class="size-3"/>
      </button>
    </div>
  </div>

  <!-- 알 수 없는 type: 원본 JSON 표시 -->
  <pre v-else class="whitespace-pre-wrap break-all p-3 font-mono text-[12px] text-foreground">{{ JSON.stringify(data, null, 2) }}</pre>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {Check, Copy} from 'lucide-vue-next'
import type {StructuredKeyValue, StructuredResult, StructuredTable} from '../utils/structuredResult'

defineProps<{ data: StructuredResult }>()

const copiedRow = ref<number | null>(null)

function isTable(d: StructuredResult): d is StructuredTable {
  return d.type === 'table' && Array.isArray((d as StructuredTable).columns)
}

function isKeyValue(d: StructuredResult): d is StructuredKeyValue {
  return d.type === 'keyvalue' && Array.isArray((d as StructuredKeyValue).items)
}

async function copyRow(text: string, index: number) {
  await navigator.clipboard.writeText(text)
  copiedRow.value = index
  setTimeout(() => {
    if (copiedRow.value === index) copiedRow.value = null
  }, 1500)
}
</script>
