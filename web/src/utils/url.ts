/**
 * 无刷新更新当前URL的查询参数（同名参数覆盖）
 * @param params 要添加/更新的参数对象
 */
export function updateQueryParams(
  params: Record<string, string | null>,
  type: 'push' | 'replace' = 'push',
): void {
  // 创建当前URL对象
  const url = new URL(window.location.href)

  // 获取当前查询参数
  const searchParams = new URLSearchParams(url.search)

  // 遍历传入的参数
  for (const [key, value] of Object.entries(params)) {
    if (value === null) {
      // 删除参数
      searchParams.delete(key)
    }
    else {
      // 设置/覆盖参数
      searchParams.set(key, value)
    }
  }

  // 更新URL的查询字符串
  url.search = searchParams.toString()

  // 无刷新更新浏览器地址栏
  if (type === 'replace') {
    window.history.replaceState(null, '', url)
  }
  else {
    window.history.pushState(null, '', url)
  }
}
