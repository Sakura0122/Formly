import type {
  EditorCreateTableForm,
  EditorFieldOption,
  EditorHeaderActionOption,
  EditorPaletteGroup,
  EditorPaletteItem,
} from '@/types/editor'

export const EDITOR_HEADER_ACTION_OPTIONS: EditorHeaderActionOption[] = [
  {
    key: 'import-schema',
    label: '导入配置',
    icon: 'solar:upload-linear',
  },
  {
    key: 'export-schema',
    label: '导出配置',
    icon: 'solar:download-linear',
  },
  {
    key: 'shortcut',
    label: '快捷说明',
    icon: 'solar:keyboard-linear',
  },
]

export const EDITOR_PALETTE_GROUPS: EditorPaletteGroup[] = [
  {
    key: 'basic',
    label: '基础输入',
    description: '覆盖大多数文本与数值录入场景',
  },
  {
    key: 'choice',
    label: '选择器',
    description: '处理单选、多选与枚举类字段',
  },
  {
    key: 'advanced',
    label: '高级',
    description: '用于时间、状态与文件等扩展能力',
  },
]

export const EDITOR_PALETTE_ITEMS: EditorPaletteItem[] = [
  {
    type: 'text',
    label: '文字',
    description: '用于展示固定文本内容',
    icon: 'mdi:format-text',
    group: 'basic',
  },
  {
    type: 'image',
    label: '图片',
    description: '用于展示固定图片内容',
    icon: 'mdi:image-outline',
    group: 'advanced',
  },
  {
    type: 'input',
    label: '单行输入',
    description: '适用于姓名、标题、编号等短文本',
    icon: 'mdi:form-textbox',
    group: 'basic',
  },
  {
    type: 'textarea',
    label: '文本域',
    description: '适用于备注、说明等长文本录入',
    icon: 'mdi:form-textarea',
    group: 'basic',
  },
  {
    type: 'number',
    label: '数字输入',
    description: '适用于金额、数量、评分等数字字段',
    icon: 'mdi:numeric',
    group: 'basic',
  },
  {
    type: 'radio',
    label: '单选框组',
    description: '在多个选项中选择唯一结果',
    icon: 'mdi:radiobox-marked',
    group: 'choice',
  },
  {
    type: 'checkbox',
    label: '多选框组',
    description: '适用于标签、权限等多值选择',
    icon: 'mdi:checkbox-marked-outline',
    group: 'choice',
  },
  {
    type: 'select',
    label: '下拉选择',
    description: '节省空间的枚举选择组件',
    icon: 'mdi:form-select',
    group: 'choice',
  },
  {
    type: 'date',
    label: '日期选择',
    description: '适用于时间点、周期和预约类字段',
    icon: 'mdi:calendar-month-outline',
    group: 'advanced',
  },
  {
    type: 'switch',
    label: '开关',
    description: '用于是/否、启用/停用等布尔状态',
    icon: 'mdi:toggle-switch-outline',
    group: 'advanced',
  },
  {
    type: 'upload',
    label: '附件上传',
    description: '用于图片、合同、资料等文件上传',
    icon: 'mdi:tray-arrow-up',
    group: 'advanced',
  },
]

export const EDITOR_DEFAULT_OPTIONS: EditorFieldOption[] = [
  {
    label: '选项一',
    value: 'option_1',
  },
  {
    label: '选项二',
    value: 'option_2',
  },
]

export const EDITOR_DEFAULT_IMAGE_URL =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="120" viewBox="0 0 320 120">
      <rect width="320" height="120" fill="#e2e8f0"/>
      <path d="M40 88l42-42 30 30 22-22 58 58H40z" fill="#94a3b8"/>
      <circle cx="232" cy="46" r="14" fill="#cbd5e1"/>
      <text x="160" y="108" text-anchor="middle" font-size="16" fill="#64748b" font-family="Arial, sans-serif">Image</text>
    </svg>`,
  )

export const EDITOR_DEFAULT_TABLE_FORM: EditorCreateTableForm = {
  rows: 3,
  columns: 3,
}

export const EDITOR_TABLE_DEFAULT_COLUMN_WIDTH = 120
export const EDITOR_TABLE_DEFAULT_ROW_HEIGHT = 25
export const EDITOR_TABLE_HEADER_HEIGHT = 25
export const EDITOR_TABLE_ROW_HEADER_WIDTH = 32
export const EDITOR_TABLE_MIN_COLUMN_WIDTH = 60
export const EDITOR_TABLE_MIN_ROW_HEIGHT = 25

export const EDITOR_TABLE_LIMITS = {
  minRows: 1,
  maxRows: 40,
  minColumns: 1,
  maxColumns: 20,
}
