<!-- eslint-disable vue/define-props-declaration -->
<script lang="ts" setup>
import type { CSSProperties } from 'vue'
import { computed } from 'vue'

const props = defineProps({
  prefix: {
    type: String,
    default: 'icon',
  },
  /**
   * 图标的名字 驼峰命名会自动转为横杠连接
   */
  name: {
    type: String,
    default: '',
  },
  /**
   * name/url 二选一，远程的优先
   * name: 本地的ICON
   * url: 远程的ICON
   */
  url: {
    type: String,
    default: '',
  },
  size: {
    type: [Number, String, Array],
    default: 16,
  },
  color: {
    type: String,
    default: 'currentColor',
  },
})
function camelToKebab(str: string) {
  return str
    .replace(/([A-Z])/g, '-$1') // 在大写字母前添加横杠
    .replace(/[_\s]+/g, '-') // 将下划线或空格替换为横杠
    .replace(/^-+|-+$/g, '') // 去掉开头和结尾的横杠
    .toLowerCase() // 转换为小写
}
const prefixCls = 'svg-icon'
const symbolId = computed(() => `#${props.prefix}-${camelToKebab(props.name || '')}`)

const getStyle = computed((): CSSProperties => {
  const size = props.size
  let w = '0'
  let h = '0'
  if (Array.isArray(size)) {
    w = `${size[0] as number}`
    h = `${size[1] as number}`
  }
  else {
    w = `${size}`
    h = `${size}`
  }
  w = `${w.replace('px', '')}px`
  h = `${h.replace('px', '')}px`
  return {
    width: w,
    height: h,
    color: !props.color || props.color === 'currentColor' ? undefined : props.color,
  }
})
</script>

<template>
  <svg :class="[prefixCls, $attrs.class]" :style="getStyle" aria-hidden="true">
    <use :xlink:href="url || symbolId" class="svg-icon" />
  </svg>
</template>

<style lang="scss" scoped>
.svg-icon {
  display: inline-block;
  overflow: hidden;
}
</style>
