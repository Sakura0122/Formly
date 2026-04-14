<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed } from 'vue'

import type { FormCatalogNode, FormEntityId } from '@/api/form-group/type'

defineOptions({
  name: 'FormManageTreeNode',
})

export type NodeAction =
  | 'create-group'
  | 'create-form'
  | 'rename-group'
  | 'delete-group'
  | 'edit-form'
  | 'rename-form'
  | 'copy-form'
  | 'move-form'
  | 'delete-form'

const props = defineProps<{
  node: FormCatalogNode
  selectedNodeId: FormEntityId | null
  selectedNodeType: 'group' | 'form' | null
}>()

const emit = defineEmits<{
  (e: 'action', payload: { action: NodeAction; node: FormCatalogNode }): void
}>()

const isGroup = computed(() => props.node.type === 'group')
const isSelected = computed(() => props.selectedNodeId === props.node.id && props.selectedNodeType === props.node.type)

const handleCommand = (command: string | number | object) => {
  emit('action', {
    action: command as NodeAction,
    node: props.node,
  })
}
</script>

<template>
  <div class="flex min-w-0 flex-1 items-center gap-2 select-none">
    <div
      :class="[
        'group flex min-w-0 flex-1 items-center gap-2 rounded-xl pr-2 text-sm text-slate-700 transition',
        isSelected ? 'text-slate-950' : '',
      ]"
    >
      <Icon
        :class="isSelected ? 'text-emerald-500' : 'text-slate-500'"
        :icon="isGroup ? 'solar:folder-with-files-bold' : 'solar:file-outline'"
        width="14"
      />

      <span class="min-w-0 flex-1 truncate text-sm">{{ node.name }}</span>

      <el-dropdown placement="bottom-end" trigger="click" @command="handleCommand" @click.stop>
        <button
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 opacity-0 transition group-hover:opacity-100"
          type="button"
        >
          <Icon icon="solar:menu-dots-bold" width="18" />
        </button>

        <template #dropdown>
          <el-dropdown-menu v-if="isGroup">
            <el-dropdown-item command="create-group">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:folder-with-files-linear" />
                <span>新建分组</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="create-form">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:add-square-linear" />
                <span>新建表单</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="rename-group">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:pen-linear" />
                <span>修改名称</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="delete-group">
              <div class="flex items-center gap-2 text-rose-500">
                <Icon class="text-base" icon="solar:trash-bin-trash-linear" />
                <span>删除分组</span>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>

          <el-dropdown-menu v-else>
            <el-dropdown-item command="edit-form">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:monitor-linear" />
                <span>设计表单</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="rename-form">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:pen-linear" />
                <span>修改名称</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="copy-form" disabled>
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-400" icon="solar:copy-linear" />
                <span>复制表单</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="move-form">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" icon="solar:round-transfer-horizontal-linear" />
                <span>移动表单</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="delete-form">
              <div class="flex items-center gap-2 text-rose-500">
                <Icon class="text-base" icon="solar:trash-bin-trash-linear" />
                <span>删除表单</span>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>
