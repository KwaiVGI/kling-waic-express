<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

// 定义图片类型接口
interface ImageItem {
  id?: string
  name?: string
  no?: string
  url?: string
  thumbnailUrl?: string
}

// Props 定义
interface Props {
  visible: boolean
  images: ImageItem[]
  currentIndex: number
}

// Emits 定义
interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'update:currentIndex', value: number): void
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  images: () => [],
  currentIndex: 0,
})

const emit = defineEmits<Emits>()

// 缩略图容器引用
const thumbnailContainer = ref<HTMLElement>()

// 当前预览的图片
const currentImage = computed(() => {
  return props.images[props.currentIndex]
})

// 关闭预览
function handleClose() {
  emit('update:visible', false)
  emit('close')
}

// 处理画布点击事件（点击空白区域关闭）
function handleCanvasClick(event: MouseEvent) {
  // 只有当点击的是画布本身（而不是其子元素）时才关闭
  if (event.target === event.currentTarget) {
    handleClose()
  }
}

// 上一张图片
function previousImage() {
  if (props.currentIndex > 0) {
    emit('update:currentIndex', props.currentIndex - 1)
  }
}

// 下一张图片
function nextImage() {
  if (props.currentIndex < props.images.length - 1) {
    emit('update:currentIndex', props.currentIndex + 1)
  }
}

// 跳转到指定图片
function jumpToImage(index: number) {
  emit('update:currentIndex', index)
}

// 图片加载完成
function onImageLoad() {
  // 可以在这里添加图片加载完成后的逻辑
}

// 滚动到当前缩略图
async function scrollToCurrentThumbnail() {
  await nextTick()
  if (thumbnailContainer.value) {
    const activeItem = thumbnailContainer.value.querySelector('.viewer-thumbnail.active') as HTMLElement
    if (activeItem) {
      activeItem.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'center',
      })
    }
  }
}

// 键盘事件处理
function handleKeydown(event: KeyboardEvent) {
  if (!props.visible)
    return

  switch (event.key) {
    case 'Escape':
      handleClose()
      break
    case 'ArrowLeft':
      previousImage()
      break
    case 'ArrowRight':
      nextImage()
      break
  }
}

// 监听 visible 变化，控制页面滚动
watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      document.body.style.overflow = 'hidden'
      scrollToCurrentThumbnail()
    }
    else {
      document.body.style.overflow = ''
    }
  },
)

// 监听当前索引变化，滚动到对应缩略图
watch(
  () => props.currentIndex,
  () => {
    scrollToCurrentThumbnail()
  },
)

// 组件挂载时添加键盘事件监听
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  // 确保恢复页面滚动
  document.body.style.overflow = ''
})
</script>

<template>
  <!-- 图片预览模态框 -->
  <Teleport to="body">
    <div v-if="visible" class="viewer-modal" @click="handleClose">
      <!-- 工具栏 -->
      <div class="viewer-toolbar" @click.stop>
        <div class="viewer-title">
          {{ currentImage?.name || currentImage?.no }}
        </div>
        <div class="viewer-counter">
          {{ currentIndex + 1 }} / {{ images.length }}
        </div>
        <button class="viewer-close" @click="handleClose">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <!-- 主图片区域 -->
      <div class="viewer-canvas" @click="handleCanvasClick">
        <!-- 导航按钮 -->
        <button
          v-if="images.length > 1 && currentIndex > 0"
          class="viewer-nav viewer-prev"
          @click.stop="previousImage"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <button
          v-if="images.length > 1 && currentIndex < images.length - 1"
          class="viewer-nav viewer-next"
          @click.stop="nextImage"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <!-- 图片容器 -->
        <div class="viewer-image-wrapper" :class="{ 'no-thumbnails': images.length <= 1 }" @click.stop>
          <img
            :src="currentImage?.url || currentImage?.thumbnailUrl"
            :alt="currentImage?.name || currentImage?.no"
            class="viewer-image"
            @load="onImageLoad"
            @click.stop
          >
        </div>
      </div>

      <!-- 底部缩略图 -->
      <div v-if="images.length > 1" class="viewer-thumbnails" @click.stop>
        <div ref="thumbnailContainer" class="viewer-thumbnail-list">
          <div
            v-for="(image, index) in images"
            :key="image.id || image.name || image.no || index"
            class="viewer-thumbnail"
            :class="{ active: index === currentIndex }"
            @click.stop="jumpToImage(index)"
          >
            <img
              :src="image.thumbnailUrl || image.url"
              :alt="image.name || image.no"
              class="viewer-thumbnail-image"
            >
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* Viewer.js 风格的图片预览器 */
.viewer-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  user-select: none;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 工具栏 */
.viewer-toolbar {
  position: relative;
  height: 60px;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  padding: 0 20px;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.viewer-title {
  flex: 1;
  color: white;
  font-size: 16px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 20px;
}

.viewer-counter {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-right: 20px;
  font-family: monospace;
}

.viewer-close {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  color: white;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.viewer-close:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 主画布区域 */
.viewer-canvas {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 0; /* 确保 flex 子元素可以收缩 */
}

/* 导航按钮 */
.viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 10;
  backdrop-filter: blur(10px);
}

.viewer-nav:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-50%) scale(1.1);
}

.viewer-prev {
  left: 30px;
}

.viewer-next {
  right: 30px;
}

/* 图片容器 */
.viewer-image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.viewer-image {
  max-width: calc(100% - 40px); /* 减去左右 padding */
  max-height: calc(100% - 40px); /* 减去上下 padding */
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  transition: opacity 0.3s ease;
}

/* 当没有缩略图时，图片可以使用更多空间 */
.viewer-image-wrapper.no-thumbnails .viewer-image {
  max-height: calc(100vh - 60px - 40px); /* 减去工具栏高度和 padding */
}

/* 确保图片在有缩略图时也能正确显示 */
.viewer-image-wrapper:not(.no-thumbnails) .viewer-image {
  max-height: calc(100vh - 60px - 100px - 40px); /* 减去工具栏、缩略图区域和 padding */
}

/* 缩略图区域 */
.viewer-thumbnails {
  height: 100px;
  background: rgba(0, 0, 0, 0.8);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 15px 20px;
}

.viewer-thumbnail-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  height: 70px;
  align-items: center;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;
}

.viewer-thumbnail-list::-webkit-scrollbar {
  height: 4px;
}

.viewer-thumbnail-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.viewer-thumbnail-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.viewer-thumbnail {
  flex-shrink: 0;
  width: 70px;
  height: 70px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  position: relative;
}

.viewer-thumbnail:hover {
  border-color: rgba(255, 255, 255, 0.5);
}

.viewer-thumbnail.active {
  border-color: #007bff;
  box-shadow: 0 0 0 1px #007bff;
}

.viewer-thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .viewer-toolbar {
    height: 50px;
    padding: 0 15px;
  }

  .viewer-title {
    font-size: 14px;
  }

  .viewer-counter {
    font-size: 12px;
    margin-right: 15px;
  }

  .viewer-close {
    width: 36px;
    height: 36px;
  }

  .viewer-nav {
    width: 44px;
    height: 44px;
  }

  .viewer-prev {
    left: 20px;
  }

  .viewer-next {
    right: 20px;
  }

  .viewer-thumbnails {
    height: 80px;
    padding: 10px 15px;
  }

  .viewer-thumbnail-list {
    height: 60px;
    gap: 8px;
  }

  .viewer-thumbnail {
    width: 60px;
    height: 60px;
  }

  .viewer-image-wrapper {
    padding: 15px;
  }

  .viewer-image {
    max-width: calc(100% - 30px);
    max-height: calc(100% - 30px);
  }

  .viewer-image-wrapper.no-thumbnails .viewer-image {
    max-height: calc(100vh - 50px - 30px);
  }

  .viewer-image-wrapper:not(.no-thumbnails) .viewer-image {
    max-height: calc(100vh - 50px - 80px - 30px);
  }
}

@media (max-width: 480px) {
  .viewer-toolbar {
    height: 45px;
    padding: 0 10px;
  }

  .viewer-title {
    font-size: 13px;
    margin-right: 10px;
  }

  .viewer-counter {
    font-size: 11px;
    margin-right: 10px;
  }

  .viewer-close {
    width: 32px;
    height: 32px;
  }

  .viewer-nav {
    width: 40px;
    height: 40px;
  }

  .viewer-prev {
    left: 15px;
  }

  .viewer-next {
    right: 15px;
  }

  .viewer-thumbnails {
    height: 70px;
    padding: 8px 10px;
  }

  .viewer-thumbnail-list {
    height: 54px;
    gap: 6px;
  }

  .viewer-thumbnail {
    width: 54px;
    height: 54px;
  }

  .viewer-image-wrapper {
    padding: 10px;
  }

  .viewer-image {
    max-width: calc(100% - 20px);
    max-height: calc(100% - 20px);
  }

  .viewer-image-wrapper.no-thumbnails .viewer-image {
    max-height: calc(100vh - 45px - 20px);
  }

  .viewer-image-wrapper:not(.no-thumbnails) .viewer-image {
    max-height: calc(100vh - 45px - 70px - 20px);
  }
}
</style>
