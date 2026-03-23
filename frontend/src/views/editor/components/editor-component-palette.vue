<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'

import { EDITOR_PALETTE_GROUPS, EDITOR_PALETTE_ITEMS } from '@/constants/editor'
import type { EditorPaletteItem } from '@/types/editor'

const props = defineProps<{
  items?: EditorPaletteItem[]
  searchPlaceholder?: string
}>()

const emit = defineEmits<{
  (e: 'select-item', item: EditorPaletteItem): void
  (e: 'drag-start', payload: { item: EditorPaletteItem; event: DragEvent }): void
}>()

const searchKeyword = ref('')

const normalizedKeyword = computed(() => searchKeyword.value.trim().toLowerCase())

const paletteItems = computed(() => props.items ?? EDITOR_PALETTE_ITEMS)

const filteredItems = computed(() => {
  if (!normalizedKeyword.value) {
    return paletteItems.value
  }

  return paletteItems.value.filter((item) => {
    const content = `${item.label} ${item.description}`.toLowerCase()
    return content.includes(normalizedKeyword.value)
  })
})

const visibleGroups = computed(() => {
  return EDITOR_PALETTE_GROUPS.map((group) => {
    return {
      ...group,
      items: filteredItems.value.filter((item) => item.group === group.key),
    }
  }).filter((group) => group.items.length > 0)
})

const handleSelectItem = (item: EditorPaletteItem) => {
  if (item.disabled) {
    return
  }

  emit('select-item', item)
}

const handleDragStart = (event: DragEvent, item: EditorPaletteItem) => {
  if (item.disabled || !event.dataTransfer) {
    return
  }

  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData(
    'application/json',
    JSON.stringify({
      type: item.type,
    }),
  )

  emit('drag-start', { item, event })
}
</script>

<template>
  <aside class="flex h-full flex-col border-r border-slate-200 bg-white">
    <div class="border-b border-slate-100 px-3 py-4">
      <div class="flex items-center justify-between gap-2">
        <h2 class="text-sm font-semibold text-slate-900">组件</h2>
        <Icon class="text-sm text-slate-400" icon="solar:widget-6-bold-duotone" />
      </div>
    </div>

    <el-scrollbar class="min-h-0 flex-1">
      <div class="px-2 py-3">
        <div v-if="visibleGroups.length" class="space-y-4">
          <section v-for="group in visibleGroups" :key="group.key" class="space-y-2">
            <div
              class="px-1 pb-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-400"
            >
              {{ group.label }}
            </div>

            <div class="grid grid-cols-1 gap-1.5">
              <button
                v-for="item in group.items"
                :key="item.type"
                class="group flex w-full flex-col items-center justify-center gap-1 rounded-lg border border-transparent px-1 py-2 text-center transition hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="item.disabled"
                draggable="true"
                type="button"
                @click="handleSelectItem(item)"
                @dragstart="handleDragStart($event, item)"
              >
                <div
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600 transition group-hover:bg-slate-900 group-hover:text-white"
                >
                  <Icon class="text-lg" :icon="item.icon" />
                </div>
                <div class="line-clamp-2 text-[11px] font-medium leading-4 text-slate-700">
                  {{ item.label }}
                </div>
              </button>
            </div>
          </section>
        </div>

        <div v-else class="flex h-full items-center justify-center py-8">
          <el-empty description="没有匹配的组件" />
        </div>
      </div>
    </el-scrollbar>
  </aside>
</template>
