import { ref } from 'vue'

type UseRequestOptions<TData> = {
  immediate?: boolean
  defaultData?: TData | null
}

export const useRequest = <TData, TArgs extends unknown[]>(
  service: (...args: TArgs) => Promise<TData>,
  options: UseRequestOptions<TData> = {},
) => {
  const loading = ref(false)
  const data = ref<TData | null>(options.defaultData ?? null)

  const run = async (...args: TArgs) => {
    loading.value = true

    try {
      const result = await service(...args)
      data.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  if (options.immediate) {
    void run(...([] as unknown as TArgs))
  }

  return {
    data,
    loading,
    run,
  }
}
