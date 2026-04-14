// 接口响应类型
export type Data<T> = {
  code: number
  data: T
  message: string
}

// 分页请求参数类型
export type PageDto<T = any> = {
  currentPage: number
  pageSize: number
} & T

// 分页响应类型
export type PageVo<T = any> = {
  list: T[]
  total: number
  pageCount: number

  [key: string]: any
}
