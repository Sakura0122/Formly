import type { FormEntityId } from '@/api/form-group/type'
import type { EditorSchema } from '@/types/editor'

export type FormDefinitionCreateReq = {
  groupId: FormEntityId | null
  name: string
  formKey: string
  description?: string
  sort?: number
}

export type FormDefinitionUpdateReq = {
  groupId?: FormEntityId | null
  name: string
  description?: string
  sort?: number
}

export type FormDefinitionEditorDetail = {
  id: FormEntityId
  groupId: FormEntityId | null
  name: string
  formKey: string
  description: string
  publishedVersionId: FormEntityId | null
  hasUnpublishedDraft: boolean
  sort: number | null
  currentSchema: EditorSchema | null
  createdAt: string
  updatedAt: string
}
