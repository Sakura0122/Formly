export type FormEntityId = string

export type FormCatalogNodeType = 'group' | 'form'

export type FormCatalogNode = {
  id: FormEntityId
  type: FormCatalogNodeType
  parentId: FormEntityId | null
  name: string
  sort: number | null
  formKey?: string | null
  createdAt: string
  updatedAt?: string | null
  children: FormCatalogNode[]
}

export type FormGroupCreateReq = {
  parentId: FormEntityId | null
  name: string
  sort?: number
}

export type FormGroupUpdateReq = {
  parentId?: FormEntityId | null
  name: string
  sort?: number
}
