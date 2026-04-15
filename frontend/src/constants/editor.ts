import type {
  EditorCreateTableForm,
  EditorFieldOption,
  EditorPaletteGroup,
  EditorPaletteItem,
} from '@/types/editor'

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
    label: '固定文字',
    description: '用于展示静态文本内容',
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
    type: 'textbox',
    label: '文本框',
    description: '适用于表格内大多数文本录入场景',
    icon: 'mdi:form-textbox',
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

export const EDITOR_DEFAULT_TABLE_FORM: EditorCreateTableForm = {
  rows: 3,
  columns: 3,
}

export const EDITOR_SCHEMA_VERSION = 1
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
