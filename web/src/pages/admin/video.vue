<template>
  <div class="control-panel">
    <div class="top-controls">
      <div class="search-container">
        <input
          type="text"
          v-model="searchQuery"
          placeholder="输入视频ID搜索..."
          @keyup.enter="searchImages"
          class="search-input"
        />
        <button @click="searchImages" class="search-button">搜索</button>
      </div>
    </div>

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
          <div class="image-container">
            <video
              class="w-full h-full"
              :src="image.url"
              controls
              preload="metadata"
            ></video>
            <span class="image-id">{{ image.name }}</span>

            <div class="item-actions">
              <button
                @click="deleteImage(image.id)"
                :disabled="
                  image.id === pinnedImageId || image.id === promotedImageId
                "
                class="action-button delete"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="pagination-controls">
        <button
          :disabled="currentPage <= 1"
          @click="goToPage(currentPage - 1)"
          class="pagination-button prev"
        >
          上一页
        </button>

        <div class="page-numbers">
          <button
            v-for="page in visiblePages"
            :key="page"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
            class="page-button"
          >
            {{ page }}
          </button>
        </div>

        <button
          :disabled="currentPage >= totalPages"
          @click="goToPage(currentPage + 1)"
          class="pagination-button next"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { castingService, type CastingImage } from "@/api/castingService";
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";

const route = useRoute();
// 数据状态
const images = ref<CastingImage[]>([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(20);
const totalPages = ref(1);
const totalImages = ref(0);
const searchQuery = ref("");
const pinnedImageId = ref<string | null>(null);
const promotedImageId = ref<string | null>(null);
const currentType = "VIDEO_EFFECT";

// 计算可见的分页按钮
const visiblePages = computed(() => {
  const pages = [];
  const maxVisible = 5;
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages.value, start + maxVisible - 1);

  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
});

// 加载图片
const loadImages = async () => {
  loading.value = true;
  try {
    const result = await castingService.getCastingList({
      keyword: searchQuery.value,
      type: currentType,
      page: currentPage.value,
      limit: pageSize.value,
    });
    images.value = result.items.map((img) => ({
      ...img,
      isPinned: img.id === pinnedImageId.value,
      isPromoted: img.id === promotedImageId.value,
    }));
    totalPages.value = Math.ceil(result.total / pageSize.value);
    totalImages.value = result.total;
  } catch (error) {
    console.error("加载图片失败:", error);
  } finally {
    loading.value = false;
  }
};

// 搜索图片
const searchImages = () => {
  currentPage.value = 1;
  loadImages();
};

// 跳转到指定页
const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  loadImages();
};

// 删除
const deleteImage = async (imageId: string) => {
  try {
    await castingService.deleteImage(currentType, imageId);
    loadImages();
  } catch (error) {
    console.error("取消固定失败:", error);
  }
};

// 初始化加载图片
onMounted(() => {
  if (route.query.token) {
    localStorage.setItem(STORAGE_TOKEN_KEY, route.query.token as string);
  }
  loadImages();
});

// 监听搜索词变化
watch(searchQuery, (newVal) => {
  if (newVal === "") {
    loadImages();
  }
});
</script>

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
  aspect-ratio: 9/16;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
  background-color: #eee;
}

.gallery-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.gallery-item.pinned {
  border: 2px solid #7209b7;
}

.gallery-item.promoted {
  border: 2px solid #ffd166;
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

.pinned-badge {
  background-color: #7209b7;
  color: white;
}

.promoted-badge {
  background-color: #ffd166;
  color: #333;
}

.item-actions {
  position: absolute;
  bottom: 6px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 2px;
  padding: 2px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
}

.action-button {
  padding: 2px 4px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 40px;
}

.action-button.promote {
  background-color: #ffd166;
  color: #333;
}

.action-button.promote:hover:not(:disabled) {
  background-color: #ffc233;
}

.action-button.promote:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.action-button.pin {
  background-color: #7209b7;
  color: white;
}

.action-button.pin:hover:not(:disabled) {
  background-color: #5f078f;
}

.action-button.pin:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.action-button.delete {
  background-color: rgb(150, 10, 10);
  color: white;
}

.action-button.delete:hover:not(:disabled) {
  background-color: rgb(189, 14, 14);
}

.action-button.delete:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.pagination-button {
  padding: 6px 12px;
  background-color: #f0f2f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
  min-width: 80px;
}

.pagination-button:not(:disabled):hover {
  background-color: #e6e9ed;
}

.pagination-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-button {
  min-width: 32px;
  padding: 6px 0;
  background-color: #f0f2f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.page-button.active {
  background-color: #4361ee;
  color: white;
  border-color: #4361ee;
}

.page-button:not(.active):hover {
  background-color: #e6e9ed;
}
</style>
