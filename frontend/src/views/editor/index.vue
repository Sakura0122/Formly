<script setup lang="ts">
import { useEventListener } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { ref, useTemplateRef } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

import { EDITOR_HEADER_ACTION_OPTIONS } from '@/constants/editor'
import { useEditorStore } from '@/stores/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorContextMenuCommand,
  EditorContextMenuItem,
  EditorHeaderActionKey,
} from '@/types/editor'
import EditorCanvas from '@/views/editor/components/editor-canvas.vue'
import EditorComponentPalette from '@/views/editor/components/editor-component-palette.vue'
import EditorConfigPanel from '@/views/editor/components/editor-config-panel.vue'
import EditorContextMenu from '@/views/editor/components/editor-context-menu.vue'
import EditorCreateTableDialog from '@/views/editor/components/editor-create-table-dialog.vue'
import EditorHeader from '@/views/editor/components/editor-header.vue'

defineOptions({
  name: 'EditorPage',
})

const router = useRouter()
const editorStore = useEditorStore()
const {
  activeCell,
  activeCellId,
  activeField,
  activeFieldId,
  canRedo,
  canUndo,
  cellFields,
  dirty,
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

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  ElMessage.info('当前没有可返回的历史页面')
}

const handlePreview = () => {
  ElMessage.info('预览能力将在后续阶段接入')
}

const saveLoading = ref(false)
const handleSave = async () => {
  saveLoading.value = true
  dirty.value = false
  await Promise.resolve()
  saveLoading.value = false
  ElMessage.success('保存事件已输出，等待后续接入真实逻辑')
}

const publishLoading = ref(false)
const handlePublish = async () => {
  publishLoading.value = true
  dirty.value = false
  await Promise.resolve()
  publishLoading.value = false
  ElMessage.success('发布事件已输出，等待后续接入真实逻辑')
}

const handleMoreAction = (action: EditorHeaderActionKey) => {
  if (action === 'shortcut') {
    ElMessage.info(
      '已支持快捷键：Ctrl/Cmd+C 复制单元格组件，Ctrl/Cmd+V 粘贴单元格组件，Ctrl/Cmd+Z 撤销，Ctrl/Cmd+Y 恢复',
    )
    return
  }

  const currentAction = EDITOR_HEADER_ACTION_OPTIONS.find((item) => item.key === action)
  ElMessage.info(`更多操作占位：${currentAction?.label ?? action}`)
}

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

const handleContextCommand = (command: EditorContextMenuCommand) => {
  if (command === 'create' || command === 'rebuild') {
    openTableDialog(command)
    return
  }

  editorStore.executeContextCommand(command)
}

const handleConfigSelectField = (fieldId: string) => {
  activeFieldId.value = fieldId
}
</script>

<template>
  <div class="flex h-screen flex-col overflow-hidden bg-slate-100">
    <EditorHeader
      :dirty="dirty"
      :publish-loading="publishLoading"
      :redo-disabled="!canRedo"
      :save-loading="saveLoading"
      :undo-disabled="!canUndo"
      status-text="草稿"
      title="Formly 表单编辑器"
      @back="handleBack"
      @more-action="handleMoreAction"
      @preview="handlePreview"
      @publish="handlePublish"
      @redo="editorStore.redo"
      @save="handleSave"
      @undo="editorStore.undo"
    />

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
          :active-cell="activeCell"
          :active-field="activeField"
          :cell-fields="cellFields"
          @change-field-type="editorStore.changeFieldType"
          @remove-field="editorStore.removeActiveField"
          @select-field="handleConfigSelectField"
          @update-field="editorStore.updateField"
        />
      </div>
    </main>

    <EditorCreateTableDialog ref="tableDialogRef" @confirm="editorStore.createTable" />

    <EditorContextMenu ref="contextMenuRef" @command="handleContextCommand" />
  </div>
</template>
