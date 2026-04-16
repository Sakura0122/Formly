import request from '@/utils/request'

import type { FormEntityId } from '@/api/form-group/type'
import type {
  FormDefinitionCreateReq,
  FormDefinitionEditorDetail,
  FormDefinitionFormDetail,
  FormDefinitionHistoryItem,
  FormDefinitionUpdateReq,
} from './type'

export const formDefinitionApi = {
  async getEditor(id: FormEntityId) {
    const res = await request.get<FormDefinitionEditorDetail>(`/form-definitions/editor/${id}`)
    return res.data
  },
  async getForm(id: FormEntityId) {
    const res = await request.get<FormDefinitionFormDetail>(`/form-definitions/form/${id}`)
    return res.data
  },
  async getHistory(id: FormEntityId) {
    const res = await request.get<FormDefinitionHistoryItem[]>(`/form-definitions/${id}/history`)
    return res.data
  },
  async create(data: FormDefinitionCreateReq) {
    const res = await request.post<FormEntityId>('/form-definitions', data)
    return res.data
  },
  async update(id: FormEntityId, data: FormDefinitionUpdateReq) {
    const res = await request.put<null>(`/form-definitions/${id}`, data)
    return res.data
  },
  async delete(id: FormEntityId) {
    const res = await request.delete<null>(`/form-definitions/${id}`)
    return res.data
  },
}
