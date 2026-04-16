<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { computed, ref, useTemplateRef, watch } from 'vue'
import { useRouter } from 'vue-router'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionFormDetail } from '@/api/form-definition/type'
import { formGroupApi } from '@/api/form-group'
import type { FormCatalogNode, FormCatalogNodeType, FormEntityId } from '@/api/form-group/type'
import FormPreviewCanvas from '@/components/form-preview/form-preview-canvas.vue'
import { useRequest } from '@/hooks/useRequest'
import FormManageCreateFormDialog from '@/views/form-manage/components/form-manage-create-form-dialog.vue'
import FormManageCreateGroupDialog from '@/views/form-manage/components/form-manage-create-group-dialog.vue'
import FormManageDeleteDialog from '@/views/form-manage/components/form-manage-delete-dialog.vue'
import FormManageMoveFormDialog from '@/views/form-manage/components/form-manage-move-form-dialog.vue'
import FormManageRenameDialog from '@/views/form-manage/components/form-manage-rename-dialog.vue'
import FormManageTreeNode, { type NodeAction } from '@/views/form-manage/components/form-manage-tree-node.vue'
import { useFormManageExportPdf } from '@/views/form-manage/useFormManageExportPdf'

defineOptions({
  name: 'FormManagePage',
})

type SelectedNode = {
  id: FormEntityId
  type: FormCatalogNodeType
}

type GroupOption = {
  id: FormEntityId
  label: string
}

type MoreAction = 'export-pdf'

const router = useRouter()

const searchKeyword = ref('')
const catalogTree = ref<FormCatalogNode[]>([])
const expandedGroupIds = ref<FormEntityId[]>([])
const selectedNodeId = ref<FormEntityId | null>(null)
const selectedNodeType = ref<FormCatalogNodeType | null>(null)
const selectedFormDetail = ref<FormDefinitionFormDetail | null>(null)
const treeRef = useTemplateRef('treeRef')
const pdfExportRef = useTemplateRef<HTMLElement>('pdfExportRef')

const collectGroupIds = (nodes: FormCatalogNode[], result: FormEntityId[] = []) => {
  nodes.forEach((node) => {
    if (node.type === 'group') {
      result.push(node.id)
      collectGroupIds(node.children, result)
    }
  })

  return result
}

const findNodeById = (nodes: FormCatalogNode[], target: SelectedNode | null): FormCatalogNode | null => {
  if (!target) {
    return null
  }

  for (const node of nodes) {
    if (node.id === target.id && node.type === target.type) {
      return node
    }

    const matchedChild = findNodeById(node.children, target)
    if (matchedChild) {
      return matchedChild
    }
  }

  return null
}

const findFirstFormNode = (nodes: FormCatalogNode[]): FormCatalogNode | null => {
  for (const node of nodes) {
    if (node.type === 'form') {
      return node
    }

    const childMatched = findFirstFormNode(node.children)
    if (childMatched) {
      return childMatched
    }
  }

  return null
}

const filterCatalogTree = (nodes: FormCatalogNode[], keyword: string): FormCatalogNode[] => {
  const normalizedKeyword = keyword.trim().toLowerCase()

  if (!normalizedKeyword) {
    return nodes
  }

  return nodes
    .map((node) => {
      const matchedChildren = filterCatalogTree(node.children, normalizedKeyword)
      const matchedSelf = node.name.toLowerCase().includes(normalizedKeyword)

      if (!matchedSelf && matchedChildren.length === 0) {
        return null
      }

      return {
        ...node,
        children: matchedChildren,
      }
    })
    .filter((node): node is FormCatalogNode => Boolean(node))
}

const flattenGroupOptions = (nodes: FormCatalogNode[], level = 0, result: GroupOption[] = []) => {
  nodes.forEach((node) => {
    if (node.type !== 'group') {
      return
    }

    result.push({
      id: node.id,
      label: `${'　'.repeat(level)}${node.name}`,
    })
    flattenGroupOptions(node.children, level + 1, result)
  })

  return result
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '--'
  }

  return value.replace('T', ' ')
}

const displayVersion = computed(() => {
  const versionNo = selectedFormDetail.value?.publishedVersionNo
  return versionNo ? `V${versionNo}` : '--'
})

const filteredCatalogTree = computed(() => filterCatalogTree(catalogTree.value, searchKeyword.value))
const groupOptions = computed(() => flattenGroupOptions(catalogTree.value))
const hasSelectedForm = computed(() => selectedNodeType.value === 'form' && Boolean(selectedFormDetail.value))
const publishedPreviewTable = computed(() => selectedFormDetail.value?.schema ?? null)
const hasPublishedPreview = computed(() => {
  return Boolean(selectedFormDetail.value?.publishedVersionNo && publishedPreviewTable.value)
})
const currentNodeKey = computed(() => selectedNodeId.value ?? undefined)

const filterTreeNode = (keyword: string, data: unknown) => {
  const currentNode = data as FormCatalogNode

  if (!keyword.trim()) {
    return true
  }

  return currentNode.name.toLowerCase().includes(keyword.trim().toLowerCase())
}

const { loading: formDetailLoading, run: fetchFormDetail } = useRequest(formDefinitionApi.getForm)
const { run: fetchEditorDetail } = useRequest(formDefinitionApi.getEditor)
const getEditorDetail = async (formId: FormEntityId) => {
  return fetchEditorDetail(formId)
}

const selectNode = async (node: FormCatalogNode | null) => {
  if (!node) {
    return
  }

  selectedNodeId.value = node.id
  selectedNodeType.value = node.type

  if (node.type !== 'form') {
    selectedFormDetail.value = null
    return
  }

  selectedFormDetail.value = await fetchFormDetail(node.id)
}

const restoreSelection = async (preferredSelection?: SelectedNode | null) => {
  const targetNode = findNodeById(catalogTree.value, preferredSelection ?? null) ?? findFirstFormNode(catalogTree.value)

  if (!targetNode) {
    selectedNodeId.value = null
    selectedNodeType.value = null
    selectedFormDetail.value = null
    return
  }

  await selectNode(targetNode)
}

const { loading: catalogTreeLoading, run: fetchCatalogTree } = useRequest(formGroupApi.getTree)
const loadCatalogTree = async (preferredSelection?: SelectedNode | null) => {
  const tree = await fetchCatalogTree()
  catalogTree.value = tree
  expandedGroupIds.value = collectGroupIds(tree)
  await restoreSelection(preferredSelection)
}
loadCatalogTree()

const handleTreeExpand = (node: FormCatalogNode) => {
  if (node.type !== 'group' || expandedGroupIds.value.includes(node.id)) {
    return
  }

  expandedGroupIds.value = [...expandedGroupIds.value, node.id]
}

const handleTreeCollapse = (node: FormCatalogNode) => {
  if (node.type !== 'group') {
    return
  }

  expandedGroupIds.value = expandedGroupIds.value.filter((id) => id !== node.id)
}

// 创建分组
const createGroupDialogRef = useTemplateRef('createGroupDialogRef')
const handleCreateGroupOpen = (node?: FormCatalogNode | null) => {
  createGroupDialogRef.value?.open({
    parentId: node?.type === 'group' ? node.id : null,
    parentName: node?.type === 'group' ? node.name : '',
  })
}

// 创建表单
const createFormDialogRef = useTemplateRef('createFormDialogRef')
const handleCreateFormOpen = (node?: FormCatalogNode | null) => {
  createFormDialogRef.value?.open({
    groupId: node?.type === 'group' ? node.id : null,
    groupName: node?.type === 'group' ? node.name : '',
  })
}

// 重命名
const renameDialogRef = useTemplateRef('renameDialogRef')
const openRenameDialog = async (node: FormCatalogNode) => {
  if (node.type === 'group') {
    renameDialogRef.value?.open({
      id: node.id,
      name: node.name,
      parentId: node.parentId,
      sort: node.sort,
      type: 'group',
    })
    return
  }

  const detail = await getEditorDetail(node.id)
  renameDialogRef.value?.open({
    description: detail.description,
    groupId: detail.groupId,
    id: detail.id,
    name: detail.name,
    sort: detail.sort,
    type: 'form',
  })
}

// 移动表单
const moveFormDialogRef = useTemplateRef('moveFormDialogRef')
const openMoveFormDialog = async (node: FormCatalogNode) => {
  const detail = await getEditorDetail(node.id)
  moveFormDialogRef.value?.open({
    description: detail.description,
    groupId: detail.groupId,
    id: detail.id,
    name: detail.name,
    sort: detail.sort,
  })
}

// 重新加载树
const handleMutationSuccess = async (payload: { id: FormEntityId; type: 'group' | 'form' }) => {
  await loadCatalogTree({
    id: payload.id,
    type: payload.type,
  })
}

const deleteDialogRef = useTemplateRef('deleteDialogRef')
const openDeleteDialog = (node: FormCatalogNode) => {
  deleteDialogRef.value?.open({
    id: node.id,
    name: node.name,
    type: node.type,
  })
}

const handleNodeAction = async (payload: { action: NodeAction; node: FormCatalogNode }) => {
  if (payload.action === 'create-group') {
    handleCreateGroupOpen(payload.node)
    return
  }

  if (payload.action === 'create-form') {
    handleCreateFormOpen(payload.node)
    return
  }

  if (['rename-group', 'rename-form'].includes(payload.action)) {
    await openRenameDialog(payload.node)
    return
  }

  if (['delete-group', 'delete-form'].includes(payload.action)) {
    openDeleteDialog(payload.node)
    return
  }

  if (payload.action === 'edit-form') {
    await selectNode(payload.node)
    router.push({
      path: '/editor',
      query: {
        formId: String(payload.node.id),
      },
    })
    return
  }

  if (payload.action === 'copy-form') {
    ElMessage.info('复制表单将在后续阶段接入')
    return
  }

  if (payload.action === 'move-form') {
    await openMoveFormDialog(payload.node)
    return
  }
}

const handleDesignForm = () => {
  if (!selectedFormDetail.value) {
    return
  }

  router.push({
    path: '/editor',
    query: {
      formId: String(selectedFormDetail.value.id),
    },
  })
}

const { handleExportPdf } = useFormManageExportPdf()

const handleMoreAction = async (command: MoreAction) => {
  if (command !== 'export-pdf') {
    return
  }

  await handleExportPdf(pdfExportRef.value, selectedFormDetail.value?.name)
}

watch(searchKeyword, (keyword) => {
  treeRef.value?.filter(keyword)
})
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-[#eef3f8] text-slate-900">
    <aside class="flex w-65 shrink-0 flex-col border-r border-slate-200 bg-white">
      <div class="border-b border-slate-100 px-5 py-5">
        <div class="flex items-center gap-3">
          <el-input v-model="searchKeyword" clearable placeholder="请输入名称">
            <template #prefix>
              <Icon class="text-slate-400" icon="solar:magnifer-linear" width="14" />
            </template>
          </el-input>

          <el-dropdown placement="bottom-end" trigger="click">
            <el-button>
              <Icon icon="ep:plus" />
            </el-button>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleCreateGroupOpen()">
                  <div class="flex items-center gap-2">
                    <Icon class="text-base text-slate-500" icon="solar:folder-with-files-linear" />
                    <span>新建分组</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item @click="handleCreateFormOpen()">
                  <div class="flex items-center gap-2">
                    <Icon class="text-base text-slate-500" icon="solar:add-square-linear" />
                    <span>新建表单</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="min-h-0 flex-1 px-4 py-4" v-loading="catalogTreeLoading">
        <div
          v-if="filteredCatalogTree.length === 0"
          class="flex h-full flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 text-center"
        >
          <Icon class="mb-3 text-slate-300" icon="solar:documents-linear" width="40" />
          <p class="text-sm text-slate-500">
            {{ searchKeyword ? '没有匹配的目录项' : '还没有表单目录，先新建一个吧' }}
          </p>
        </div>

        <el-scrollbar v-else class="h-full">
          <el-tree
            ref="treeRef"
            :current-node-key="currentNodeKey"
            :data="catalogTree"
            :default-expanded-keys="expandedGroupIds"
            :expand-on-click-node="false"
            :filter-node-method="filterTreeNode"
            :highlight-current="true"
            node-key="id"
            @current-change="selectNode"
            @node-collapse="handleTreeCollapse"
            @node-expand="handleTreeExpand"
          >
            <template #default="{ data }">
              <FormManageTreeNode
                :node="data"
                :selected-node-id="selectedNodeId"
                :selected-node-type="selectedNodeType"
                @action="handleNodeAction"
              />
            </template>
          </el-tree>
        </el-scrollbar>
      </div>
    </aside>

    <section class="min-w-0 flex-1 overflow-hidden">
      <div class="flex h-full flex-col">
        <header class="flex items-center justify-between border-b border-slate-200 bg-white px-8 py-5">
          <div v-if="selectedFormDetail" class="flex min-w-0 flex-wrap items-center gap-x-8 gap-y-3">
            <div class="flex items-center gap-1">
              <span class="text-sm text-slate-400">绑定ID：</span>
              <span class="text-[15px] font-medium text-slate-800">{{ selectedFormDetail.formKey }}</span>
            </div>

            <div class="flex items-center gap-1">
              <span class="text-sm text-slate-400">版本：</span>
              <el-tag>{{ displayVersion }}</el-tag>
            </div>

            <div class="flex items-center gap-1">
              <span class="text-sm text-slate-400">创建人：</span>
              <el-tag type="info">
                <div class="flex items-center gap-1">
                  <Icon class="text-base" icon="ep:user-filled" width="12" />
                  sakura
                </div>
              </el-tag>
            </div>

            <div class="flex items-center gap-1 text-sm">
              <span class="text-slate-400">更新时间：</span>
              <span class="text-slate-700">{{ formatDateTime(selectedFormDetail.updatedAt) }}</span>
            </div>
          </div>

          <div v-else class="text-sm text-slate-400">请选择左侧表单查看详情</div>

          <div class="flex items-center">
            <el-dropdown
              :disabled="!hasPublishedPreview"
              placement="bottom-end"
              trigger="hover"
              @command="handleMoreAction"
            >
              <div class="form-manage-more-trigger cursor-pointer text-slate-500 hover:text-slate-700">
                <Icon class="text-base" icon="solar:menu-dots-bold" />
              </div>

              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="export-pdf">导出 PDF</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-button disabled plain>
              <Icon class="mr-1 text-base" icon="solar:settings-linear" />
              数据管理
            </el-button>
            <el-button :disabled="!hasSelectedForm" type="primary" @click="handleDesignForm">
              <Icon class="mr-1 text-base" icon="solar:pen-new-square-linear" />
              设计表单
            </el-button>
          </div>
        </header>

        <div v-loading="formDetailLoading" class="min-h-0 flex-1 p-5">
          <div
            v-if="!selectedFormDetail"
            class="flex h-full min-h-120 flex-col items-center justify-center border border-dashed border-slate-200 bg-white/80 text-center"
          >
            <Icon class="mb-4 text-slate-300" icon="solar:document-add-linear" width="54" />
            <p class="text-lg font-medium text-slate-700">选择一个表单开始管理</p>
            <p class="mt-2 text-sm text-slate-400">这里会展示表单基础信息和预览区域</p>
          </div>

          <div v-else class="h-full overflow-hidden bg-[#f5f8fc] px-4 shadow-[0_18px_48px_rgba(148,163,184,0.08)]">
            <el-scrollbar v-if="hasPublishedPreview" class="h-full">
              <div class="flex min-h-full items-start justify-center py-4">
                <div class="shrink-0 border border-slate-200 bg-white p-8 shadow-[0_10px_36px_rgba(148,163,184,0.12)]">
                  <FormPreviewCanvas :table="publishedPreviewTable" mode="readonly" />
                </div>
              </div>
            </el-scrollbar>

            <div v-else class="flex h-full min-h-120 flex-col items-center justify-center text-center">
              <Icon class="mb-5 text-slate-300" icon="solar:document-text-linear" width="58" />
              <p class="text-lg font-medium text-slate-700">当前暂无已发布版本</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div
      v-if="hasPublishedPreview"
      ref="pdfExportRef"
      class="pointer-events-none fixed top-0 w-max bg-white p-8"
      style="left: -20000px"
    >
      <FormPreviewCanvas :table="publishedPreviewTable" mode="readonly" scene="print" />
    </div>

    <FormManageCreateGroupDialog ref="createGroupDialogRef" @success="handleMutationSuccess" />

    <FormManageCreateFormDialog ref="createFormDialogRef" @success="handleMutationSuccess" />

    <FormManageRenameDialog ref="renameDialogRef" @success="handleMutationSuccess" />

    <FormManageMoveFormDialog ref="moveFormDialogRef" :options="groupOptions" @success="handleMutationSuccess" />

    <FormManageDeleteDialog ref="deleteDialogRef" @success="handleMutationSuccess" />
  </div>
</template>

<style scoped>
.form-manage-more-trigger {
  outline: none;
  box-shadow: none;
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.form-manage-more-trigger:focus,
.form-manage-more-trigger:focus-visible,
.form-manage-more-trigger:active {
  outline: none;
  box-shadow: none;
}
</style>
