<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { castingService } from '@/api/castingService'
import type { CastingImage } from '@/api/castingService'

import { showToast } from 'vant'
import { useRoute } from 'vue-router'
import ImagePreview from '@/components/ImagePreview.vue'

const route = useRoute()
// 数据状态
const images = ref<CastingImage[]>([])
const loading = ref(false)
const searchQuery = ref('')
const pinnedImageId = ref<string | null>(null)
const promotedImageId = ref<string | null>(null)

// 图片预览相关状态
const showPreview = ref(false)
const currentPreviewIndex = ref(0)

// 加载图片
async function loadImages() {
  loading.value = true
  try {
    const result = await castingService.getPrintList(searchQuery.value)
    images.value = result
  }
  catch (error) {
    console.error('加载图片失败:', error)
    showToast(`加载图片失败，请重试${error}`)
  }
  finally {
    loading.value = false
  }
}

// 搜索图片
function searchImages() {
  loadImages()
}

// 图片预览相关方法
function openImagePreview(image: CastingImage, index: number) {
  currentPreviewIndex.value = index
  showPreview.value = true
}

function closeImagePreview() {
  showPreview.value = false
}

// 初始化加载图片
onMounted(() => {
  if (route.query.token) {
    // Token handling removed
  }
  loadImages()
})

// 监听搜索词变化
watch(searchQuery, (newVal) => {
  if (newVal === '') {
    loadImages()
  }
})
</script>

<template>
  <div class="control-panel">
    <div class="top-controls">
      <div class="search-container">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="输入图片ID搜索..."
          class="search-input"
          @keyup.enter="searchImages"
        >
        <button class="search-button" @click="searchImages">
          搜索
        </button>
      </div>
    </div>
    <p class="text-12px">
      当前的打印队列：{{ images.length }}
    </p>
    <div class="image-gallery">
      <div v-if="loading" class="loading-message">
        <span class="loading-text">加载中...</span>
      </div>

      <div v-else-if="images.length === 0" class="empty-message">
        <span class="empty-text">没有找到匹配的视频</span>
      </div>

      <div v-else class="gallery-grid">
        <div
          v-for="image in images"
          :key="image.id"
          class="gallery-item"
          :class="{
            pinned: image.id === pinnedImageId,
            promoted: image.id === promotedImageId,
          }"
        >
          <div class="image-container" @click="openImagePreview(image, images.indexOf(image))">
            <img class="h-full w-full" :src="image.url">
            <span class="image-id">{{ image.no }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览组件 -->
    <ImagePreview
      v-model:visible="showPreview"
      v-model:current-index="currentPreviewIndex"
      :images="images"
      @close="closeImagePreview"
    />
  </div>
</template>

<style scoped>
.control-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.top-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 250px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  flex: 1;
  min-width: 180px;
  outline: none;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #4361ee;
}

.search-button {
  padding: 8px 16px;
  background-color: #4361ee;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
  white-space: nowrap;
}

.search-button:hover {
  background-color: #3a56d4;
}

.display-status {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 14px;
}

.status-label {
  color: #666;
}

.status-value {
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
}

.status-value.carousel {
  color: #4361ee;
  background-color: rgba(67, 97, 238, 0.1);
}

.status-value.fix {
  color: #7209b7;
  background-color: rgba(114, 9, 183, 0.1);
}

.pinned-image-id,
.promoted-image-id {
  font-family: monospace;
  background-color: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.mode-button,
.preview-button {
  padding: 6px 12px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.mode-button {
  background-color: #f0f2f5;
  border: 1px solid #ddd;
  color: #666;
}

.mode-button:not(:disabled):hover {
  background-color: #e6e9ed;
}

.mode-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.preview-button {
  background-color: #7209b7;
  color: white;
  text-decoration: none;
  border: none;
}

.preview-button:hover {
  background-color: #5f078f;
}

.image-gallery {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.loading-message,
.empty-message {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
  flex: 1;
}

.loading-text,
.empty-text {
  font-size: 14px;
  color: #666;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.gallery-item {
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.3s,
    box-shadow 0.3s;
  background-color: #eee;
}

.gallery-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.image-container {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.image-id {
  position: absolute;
  top: 6px;
  left: 6px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-family: monospace;
  max-width: calc(100% - 12px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-badges {
  position: absolute;
  top: 6px;
  right: 6px;
  display: flex;
  gap: 4px;
}

.badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

/* 添加图片容器的鼠标指针样式 */
.image-container {
  cursor: pointer;
}
</style>
