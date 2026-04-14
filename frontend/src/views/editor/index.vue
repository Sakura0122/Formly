<script setup lang="ts">
import { useEditorStore } from '@/stores/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorContextMenuActionPayload,
  EditorContextMenuItem
} from '@/types/editor'
import EditorCanvas from '@/views/editor/components/editor-canvas.vue'
import EditorComponentPalette from '@/views/editor/components/editor-component-palette.vue'
import EditorConfigPanel from '@/views/editor/components/editor-config-panel.vue'
import EditorContextMenu from '@/views/editor/components/editor-context-menu.vue'
import EditorCreateTableDialog from '@/views/editor/components/editor-create-table-dialog.vue'
import EditorHeader from '@/views/editor/components/editor-header.vue'
import { useEventListener } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useTemplateRef } from 'vue'

defineOptions({
  name: 'EditorPage',
})

const editorStore = useEditorStore()
const {
  activeCell,
  activeCellId,
  activeField,
  activeFieldId,
  cellFields,
  selectedCellIds,
  selectionAnchorCellId,
  table,
} = storeToRefs(editorStore)

const tableDialogRef = useTemplateRef<InstanceType<typeof EditorCreateTableDialog>>('tableDialogRef')
const openTableDialog = (mode: 'create' | 'rebuild') => {
  tableDialogRef.value?.open(mode)
}

const contextMenuRef = useTemplateRef<InstanceType<typeof EditorContextMenu>>('contextMenuRef')
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
  if (table.value && payload.cellId && !selectedCellIds.value.includes(payload.cellId)) {
    editorStore.collapseSelectionToCell(payload.cellId)
  }

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

const handleConfigSelectField = (fieldId: string) => {
  activeFieldId.value = fieldId
}
</script>

<template>
  <div class="flex h-screen flex-col overflow-hidden bg-slate-100">
    <EditorHeader />

    <main class="flex min-h-0 flex-1 gap-4 overflow-hidden p-4 lg:p-5">
      <div class="min-h-0 w-29 shrink-0">
        <EditorComponentPalette @select-item="editorStore.selectItem" />
      </div>

      <div class="min-w-0 min-h-0 flex-1">
        <EditorCanvas
          :active-cell-id="activeCellId"
          :active-field-id="activeFieldId"
          :selected-cell-ids="selectedCellIds"
          :selection-anchor-cell-id="selectionAnchorCellId"
          :table="table"
          @change-selection="editorStore.applySelection"
          @clear-selection="editorStore.resetSelection"
          @context-menu="handleCanvasContextMenu"
          @place-item="editorStore.placeItem"
          @resize-column="editorStore.resizeColumn"
          @resize-row="editorStore.resizeRow"
          @select-field="editorStore.selectField"
        />
      </div>

      <div class="min-h-0 w-[320px] shrink-0">
        <EditorConfigPanel
          @add-option="editorStore.addActiveFieldOption"
          :active-cell="activeCell"
          :active-field="activeField"
          :cell-fields="cellFields"
          @change-field-type="editorStore.changeFieldType"
          @remove-field="editorStore.removeActiveField"
          @remove-option="editorStore.removeActiveFieldOption"
          @select-field="handleConfigSelectField"
          @update-field="editorStore.updateField"
          @update-option="editorStore.updateActiveFieldOption"
        />
      </div>
    </main>

    <EditorCreateTableDialog ref="tableDialogRef" @confirm="editorStore.createTable" />

    <EditorContextMenu ref="contextMenuRef" @command="handleContextCommand" />
  </div>
</template>
