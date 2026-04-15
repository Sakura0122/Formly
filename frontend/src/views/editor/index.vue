<script setup lang="ts">
import { formDefinitionApi } from '@/api/form-definition'
import { useRequest } from '@/hooks/useRequest'
import { useEditorStore } from '@/stores/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorContextMenuActionPayload,
  EditorContextMenuItem,
} from '@/types/editor'
import EditorCanvas from '@/views/editor/components/editor-canvas.vue'
import EditorComponentPalette from '@/views/editor/components/editor-component-palette.vue'
import EditorConfigPanel from '@/views/editor/components/editor-config-panel.vue'
import EditorContextMenu from '@/views/editor/components/editor-context-menu.vue'
import EditorCreateTableDialog from '@/views/editor/components/editor-create-table-dialog.vue'
import EditorHeader from '@/views/editor/components/editor-header.vue'
import { useEventListener } from '@vueuse/core'
import { onBeforeUnmount, ref, useTemplateRef, watch } from 'vue'
import { useRoute } from 'vue-router'

defineOptions({
  name: 'EditorPage',
})

const editorStore = useEditorStore()
const route = useRoute()

const tableDialogRef = useTemplateRef('tableDialogRef')
const openTableDialog = (mode: 'create' | 'rebuild') => {
  tableDialogRef.value?.open(mode)
}

const contextMenuRef = useTemplateRef('contextMenuRef')
const openContextMenu = (
  position: {
    x: number
    y: number
  },
  items: EditorContextMenuItem[],
) => {
  contextMenuRef.value?.open(position, items)
}

/**
 * 快捷键只留在页面层，避免 store 直接依赖浏览器事件。
 */
const isEditableTarget = (target: EventTarget | null) => {
  return target instanceof HTMLElement
    ? Boolean(target.closest('input, textarea, select, [contenteditable="true"]'))
    : false
}

const handleWindowKeydown = (event: KeyboardEvent) => {
  if (
    event.isComposing ||
    event.defaultPrevented ||
    !(event.metaKey || event.ctrlKey) ||
    event.shiftKey ||
    event.altKey ||
    isEditableTarget(event.target)
  ) {
    return
  }

  const key = event.key.toLowerCase()

  if (key === 'c') {
    if (editorStore.copyActiveCell()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'v') {
    if (editorStore.pasteToActiveCell()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'z') {
    if (editorStore.undo()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'y' && editorStore.redo()) {
    event.preventDefault()
  }
}

useEventListener(window, 'keydown', handleWindowKeydown)

const handleCanvasContextMenu = (payload: EditorCanvasContextMenuPayload) => {
  openContextMenu(
    {
      x: payload.x,
      y: payload.y,
    },
    editorStore.getContextMenuItems(),
  )
}

const handleContextCommand = (payload: EditorContextMenuActionPayload) => {
  if (payload.command === 'create' || payload.command === 'rebuild') {
    openTableDialog(payload.command)
    return
  }

  editorStore.executeContextCommand(payload)
}

const { loading: pageLoading, run: getEditorApi } = useRequest(formDefinitionApi.getEditor)
const loadEditor = async (formId: string) => {
  if (!formId) {
    editorStore.resetEditorSession()
    return
  }

  pageLoading.value = true
  editorStore.resetEditorSession()

  const editorDetail = await getEditorApi(formId)
  editorStore.hydrateEditor(editorDetail)
}

watch(
  () => route.query.formId,
  (formId) => {
    loadEditor(formId as string)
  },
  {
    immediate: true,
  },
)

onBeforeUnmount(() => {
  editorStore.resetEditorSession()
})
</script>

<template>
  <div v-loading="pageLoading" class="flex h-screen flex-col overflow-hidden bg-slate-100">
    <EditorHeader />

    <main class="flex min-h-0 flex-1 gap-4 overflow-hidden p-4 lg:p-5">
      <div class="min-h-0 w-29 shrink-0">
        <EditorComponentPalette @select-item="editorStore.selectItem" />
      </div>

      <div class="min-w-0 min-h-0 flex-1">
        <EditorCanvas @context-menu="handleCanvasContextMenu" />
      </div>

      <div class="min-h-0 w-80 shrink-0">
        <EditorConfigPanel />
      </div>
    </main>

    <EditorCreateTableDialog ref="tableDialogRef" @confirm="editorStore.createTable" />

    <EditorContextMenu ref="contextMenuRef" @command="handleContextCommand" />
  </div>
</template>
