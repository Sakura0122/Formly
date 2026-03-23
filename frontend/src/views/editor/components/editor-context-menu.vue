<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onClickOutside, useElementSize, useWindowSize } from '@vueuse/core'
import { nextTick, ref, useTemplateRef, watch } from 'vue'

import type { EditorContextMenuCommand, EditorContextMenuItem } from '@/types/editor'

const emit = defineEmits<{
  (e: 'command', command: EditorContextMenuCommand): void
}>()

const menuRef = useTemplateRef<HTMLDivElement>('menuRef')
const visible = ref(false)
const items = ref<EditorContextMenuItem[]>([])
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
  coordinateX.value = coordinate.x
  coordinateY.value = coordinate.y
  menuLeft.value = coordinate.x
  menuTop.value = coordinate.y
  visible.value = true

  await nextTick()
  syncMenuPosition()
}

const handleCommand = (command: EditorContextMenuCommand) => {
  emit('command', command)
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
      <button
        v-for="item in items"
        :key="item.command"
        class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="item.disabled"
        type="button"
        @click="handleCommand(item.command)"
      >
        <Icon class="text-base text-slate-500" :icon="item.icon" />
        <span>{{ item.label }}</span>
      </button>
    </div>
  </Teleport>
</template>
