<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

import type { EditorContextMenuCommand, EditorContextMenuItem } from '@/types/editor'

const emit = defineEmits<{
  (e: 'command', command: EditorContextMenuCommand): void
}>()

const menuRef = ref<HTMLDivElement>()
const visible = ref(false)
const items = ref<EditorContextMenuItem[]>([])
const menuLeft = ref(0)
const menuTop = ref(0)

const close = () => {
  visible.value = false
}

const open = async (
  coordinate: {
    x: number
    y: number
  },
  nextItems: EditorContextMenuItem[],
) => {
  items.value = nextItems
  menuLeft.value = coordinate.x
  menuTop.value = coordinate.y
  visible.value = true

  await nextTick()

  const menuWidth = menuRef.value?.offsetWidth ?? 0
  const menuHeight = menuRef.value?.offsetHeight ?? 0
  const safePadding = 12

  menuLeft.value = Math.min(
    coordinate.x,
    window.innerWidth - menuWidth - safePadding,
  )
  menuTop.value = Math.min(
    coordinate.y,
    window.innerHeight - menuHeight - safePadding,
  )
}

const handleCommand = (command: EditorContextMenuCommand) => {
  emit('command', command)
  close()
}

const handleWindowPointerDown = (event: PointerEvent) => {
  if (!visible.value) {
    return
  }

  if (menuRef.value?.contains(event.target as Node)) {
    return
  }

  close()
}

const handleWindowResize = () => {
  if (!visible.value) {
    return
  }

  close()
}

onMounted(() => {
  window.addEventListener('pointerdown', handleWindowPointerDown)
  window.addEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', handleWindowPointerDown)
  window.removeEventListener('resize', handleWindowResize)
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
      class="fixed z-50 min-w-[148px] overflow-hidden rounded-xl border border-slate-200 bg-white p-1.5 shadow-[0_16px_40px_rgba(15,23,42,0.14)]"
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
        <Icon
          class="text-base text-slate-500"
          :icon="item.icon"
        />
        <span>{{ item.label }}</span>
      </button>
    </div>
  </Teleport>
</template>
