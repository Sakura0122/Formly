import type { FormEntityId } from '@/api/form-group/type'

export type FormDefinitionDetail = {
  id: FormEntityId
  groupId: FormEntityId | null
  name: string
  formKey: string
  description: string
  currentVersionId: FormEntityId | null
  publishedVersionId: FormEntityId | null
  sort: number | null
  createdBy: string
  createdAt: string
  updatedAt: string
}

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
