export function waitWithAbort(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    // 如果已经取消，立即拒绝
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }

    const timer = setTimeout(() => {
      resolve()
      // 清理事件监听
      if (signal) {
        signal.removeEventListener('abort', handleAbort)
      }
    }, ms)

    // 取消事件处理函数
    const handleAbort = () => {
      clearTimeout(timer)
      reject(new DOMException('Aborted', 'AbortError'))
    }

    // 监听取消信号
    if (signal) {
      signal.addEventListener('abort', handleAbort)
    }
  })
}

export async function wait(delay: number) {
  return new Promise(resolve => setTimeout(resolve, delay))
}
