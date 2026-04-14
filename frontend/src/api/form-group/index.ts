import request from '@/utils/request'

import type { FormCatalogNode, FormEntityId, FormGroupCreateReq, FormGroupUpdateReq } from './type'

export const formGroupApi = {
  async getTree() {
    const res = await request.get<FormCatalogNode[]>('/form-groups/tree')
    return res.data
  },
  async create(data: FormGroupCreateReq) {
    const res = await request.post<FormEntityId>('/form-groups', data)
    return res.data
  },
  async update(id: FormEntityId, data: FormGroupUpdateReq) {
    const res = await request.put<null>(`/form-groups/${id}`, data)
    return res.data
  },
  async delete(id: FormEntityId) {
    const res = await request.delete<null>(`/form-groups/${id}`)
    return res.data
  },
}
