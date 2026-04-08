<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onClickOutside, useElementSize, useWindowSize } from '@vueuse/core'
import { nextTick, ref, useTemplateRef, watch } from 'vue'

import type {
  EditorContextMenuActionPayload,
  EditorContextMenuCommand,
  EditorContextMenuCountInput,
  EditorContextMenuItem,
} from '@/types/editor'

const emit = defineEmits<{
  (e: 'command', payload: EditorContextMenuActionPayload): void
}>()

const menuRef = useTemplateRef<HTMLDivElement>('menuRef')
const visible = ref(false)
const items = ref<EditorContextMenuItem[]>([])
const countByCommand = ref<Partial<Record<EditorContextMenuCommand, number>>>({})
const coordinateX = ref(0)
const coordinateY = ref(0)
const menuLeft = ref(0)
const menuTop = ref(0)
const safePadding = 12

const { width: windowWidth, height: windowHeight } = useWindowSize()
const { width: menuWidth, height: menuHeight } = useElementSize(menuRef)

const close = () => {
  visible.value = false
}

const getInputMax = (countInput: EditorContextMenuCountInput) => {
  return Math.max(countInput.min, countInput.max)
}

const normalizeCount = (value: number | null | undefined, countInput: EditorContextMenuCountInput) => {
  const rawValue = Number.isFinite(value) ? Number(value) : countInput.defaultValue
  const nextValue = Math.floor(rawValue)

  return Math.min(getInputMax(countInput), Math.max(countInput.min, nextValue))
}

const syncCommandCounts = (nextItems: EditorContextMenuItem[]) => {
  countByCommand.value = nextItems.reduce<Partial<Record<EditorContextMenuCommand, number>>>((result, item) => {
    if (!item.countInput) {
      return result
    }

    result[item.command] = normalizeCount(item.countInput.defaultValue, item.countInput)
    return result
  }, {})
}

const syncMenuPosition = () => {
  if (!visible.value) {
    return
  }

  const maxLeft = Math.max(safePadding, windowWidth.value - menuWidth.value - safePadding)
  const maxTop = Math.max(safePadding, windowHeight.value - menuHeight.value - safePadding)

  menuLeft.value = Math.max(safePadding, Math.min(coordinateX.value, maxLeft))
  menuTop.value = Math.max(safePadding, Math.min(coordinateY.value, maxTop))
}

const open = async (
  coordinate: {
    x: number
    y: number
  },
  nextItems: EditorContextMenuItem[],
) => {
  items.value = nextItems
  syncCommandCounts(nextItems)
  coordinateX.value = coordinate.x
  coordinateY.value = coordinate.y
  menuLeft.value = coordinate.x
  menuTop.value = coordinate.y
  visible.value = true

  await nextTick()
  syncMenuPosition()
}

const getCommandCount = (item: EditorContextMenuItem) => {
  return item.countInput ? countByCommand.value[item.command] ?? item.countInput.defaultValue : 1
}

const updateCommandCount = (item: EditorContextMenuItem, value: number | null | undefined) => {
  if (!item.countInput) {
    return
  }

  countByCommand.value[item.command] = normalizeCount(value, item.countInput)
}

const handleCommand = (item: EditorContextMenuItem) => {
  emit('command', {
    command: item.command,
    count: getCommandCount(item),
  })
  close()
}

watch([visible, coordinateX, coordinateY, windowWidth, windowHeight, menuWidth, menuHeight], () => {
  syncMenuPosition()
})

onClickOutside(menuRef, () => {
  if (!visible.value) {
    return
  }

  close()
})

defineExpose({
  open,
  close,
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="fixed z-50 min-w-37 overflow-hidden rounded-xl border border-slate-200 bg-white p-1.5 shadow-[0_16px_40px_rgba(15,23,42,0.14)]"
      :style="{
        left: `${menuLeft}px`,
        top: `${menuTop}px`,
      }"
      @contextmenu.prevent
    >
      <template v-for="item in items" :key="item.command">
        <button
          v-if="!item.countInput"
          class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs text-slate-700 transition hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="item.disabled"
          type="button"
          @click="handleCommand(item)"
        >
          <Icon class="text-base text-slate-500" :icon="item.icon" />
          <span>{{ item.label }}</span>
        </button>

        <div
          v-else
          class="flex items-center gap-2 rounded-lg px-3 py-2 transition hover:bg-slate-50"
          :class="item.disabled ? 'opacity-50' : ''"
        >
          <button
            class="flex min-w-0 flex-1 items-center gap-2 text-left text-xs text-slate-700 transition hover:text-slate-900 disabled:cursor-not-allowed"
            :disabled="item.disabled"
            type="button"
            @click="handleCommand(item)"
          >
            <Icon class="shrink-0 text-base text-slate-500" :icon="item.icon" />
            <span class="truncate">{{ item.label }}</span>
          </button>

          <div class="flex shrink-0 items-center gap-2" @click.stop @mousedown.stop @keydown.stop>
            <el-input-number
              :model-value="getCommandCount(item)"
              :disabled="item.disabled"
              :max="getInputMax(item.countInput)"
              :min="item.countInput.min"
              :precision="0"
              :step="1"
              controls-position="right"
              size="small"
              style="width: 60px"
              @update:model-value="updateCommandCount(item, $event)"
            />
            <span class="text-xs text-slate-500">{{ item.countInput.unit }}</span>
          </div>
        </div>
      </template>
    </div>
  </Teleport>
</template>
