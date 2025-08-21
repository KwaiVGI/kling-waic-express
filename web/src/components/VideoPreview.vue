<template>
  <!-- 视频预览模态框 -->
  <Teleport to="body">
    <div v-if="visible" class="video-viewer-modal" @click="handleClose">
      <!-- 工具栏 -->
      <div class="video-viewer-toolbar" @click.stop>
        <div class="video-viewer-title">
          {{ currentVideo?.name || currentVideo?.no }}
        </div>
        <div class="video-viewer-counter">
          {{ currentIndex + 1 }} / {{ videos.length }}
        </div>
        <button class="video-viewer-close" @click="handleClose">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M18 6L6 18M6 6l12 12"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </div>

      <!-- 主视频区域 -->
      <div class="video-viewer-canvas" @click="handleCanvasClick">
        <!-- 导航按钮 -->
        <button
          v-if="videos.length > 1 && currentIndex > 0"
          class="video-viewer-nav video-viewer-prev"
          @click.stop="previousVideo"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M15 18l-6-6 6-6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>

        <button
          v-if="videos.length > 1 && currentIndex < videos.length - 1"
          class="video-viewer-nav video-viewer-next"
          @click.stop="nextVideo"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M9 18l6-6-6-6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>

        <!-- 视频容器 -->
        <div
          class="video-viewer-video-wrapper"
          :class="{ 'no-thumbnails': videos.length <= 1 }"
          @click.stop
        >
          <video
            ref="videoElement"
            :src="currentVideo?.url"
            :poster="currentVideo?.poster"
            class="video-viewer-video"
            controls
            preload="metadata"
            autoplay
            loop
            @loadedmetadata="onVideoLoad"
            @loadeddata="autoPlayCurrentVideo"
            @click.stop
          />
        </div>
      </div>

      <!-- 底部缩略图 -->
      <div v-if="videos.length > 1" class="video-viewer-thumbnails" @click.stop>
        <div ref="thumbnailContainer" class="video-viewer-thumbnail-list">
          <div
            v-for="(video, index) in videos"
            :key="video.id || video.name || video.no || index"
            class="video-viewer-thumbnail"
            :class="{ active: index === currentIndex }"
            @click.stop="jumpToVideo(index)"
          >
            <video
              :src="video.url"
              :poster="video.poster"
              class="video-viewer-thumbnail-video"
              preload="metadata"
              muted
            />
            <div class="video-play-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M8 5v14l11-7z" fill="currentColor" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch, ref, nextTick } from "vue";

// 定义视频类型接口
interface VideoItem {
  id?: string;
  name?: string;
  no?: string;
  url?: string;
  poster?: string;
}

// Props 定义
interface Props {
  visible: boolean;
  videos: VideoItem[];
  currentIndex: number;
}

// Emits 定义
interface Emits {
  (e: "update:visible", value: boolean): void;
  (e: "update:currentIndex", value: number): void;
  (e: "close"): void;
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  videos: () => [],
  currentIndex: 0,
});

const emit = defineEmits<Emits>();

// 视频元素引用
const videoElement = ref<HTMLVideoElement>();
// 缩略图容器引用
const thumbnailContainer = ref<HTMLElement>();

// 当前预览的视频
const currentVideo = computed(() => {
  return props.videos[props.currentIndex];
});

// 关闭预览
const handleClose = () => {
  // 暂停当前视频
  if (videoElement.value) {
    videoElement.value.pause();
  }
  emit("update:visible", false);
  emit("close");
};

// 处理画布点击事件（点击空白区域关闭）
const handleCanvasClick = (event: MouseEvent) => {
  // 只有当点击的是画布本身（而不是其子元素）时才关闭
  if (event.target === event.currentTarget) {
    handleClose();
  }
};

// 上一个视频
const previousVideo = () => {
  if (props.currentIndex > 0) {
    emit("update:currentIndex", props.currentIndex - 1);
  }
};

// 下一个视频
const nextVideo = () => {
  if (props.currentIndex < props.videos.length - 1) {
    emit("update:currentIndex", props.currentIndex + 1);
  }
};

// 跳转到指定视频
const jumpToVideo = (index: number) => {
  emit("update:currentIndex", index);
};

// 视频加载完成后自动播放
const onVideoLoad = () => {
  if (videoElement.value && props.visible) {
    videoElement.value.play().catch((error) => {
      console.log("自动播放失败:", error);
    });
  }
};

// 自动播放当前视频
const autoPlayCurrentVideo = async () => {
  await nextTick();
  if (videoElement.value && props.visible) {
    try {
      await videoElement.value.play();
    } catch (error) {
      console.log("自动播放失败:", error);
    }
  }
};

// 滚动到当前缩略图
const scrollToCurrentThumbnail = async () => {
  await nextTick();
  if (thumbnailContainer.value) {
    const activeItem = thumbnailContainer.value.querySelector(
      ".video-viewer-thumbnail.active"
    ) as HTMLElement;
    if (activeItem) {
      activeItem.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "center",
      });
    }
  }
};

// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  if (!props.visible) return;

  switch (event.key) {
    case "Escape":
      handleClose();
      break;
    case "ArrowLeft":
      previousVideo();
      break;
    case "ArrowRight":
      nextVideo();
      break;
    case " ": // 空格键暂停/播放
      event.preventDefault();
      if (videoElement.value) {
        if (videoElement.value.paused) {
          videoElement.value.play();
        } else {
          videoElement.value.pause();
        }
      }
      break;
  }
};

// 监听 visible 变化，控制页面滚动和自动播放
watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      document.body.style.overflow = "hidden";
      scrollToCurrentThumbnail();
      // 延迟一下确保视频元素已经渲染
      setTimeout(() => {
        autoPlayCurrentVideo();
      }, 100);
    } else {
      document.body.style.overflow = "";
      // 关闭时暂停视频
      if (videoElement.value) {
        videoElement.value.pause();
      }
    }
  }
);

// 监听当前索引变化，滚动到对应缩略图并自动播放新视频
watch(
  () => props.currentIndex,
  () => {
    // 暂停之前的视频
    if (videoElement.value) {
      videoElement.value.pause();
    }
    scrollToCurrentThumbnail();
    // 延迟一下确保新视频已经加载
    setTimeout(() => {
      autoPlayCurrentVideo();
    }, 100);
  }
);

// 组件挂载时添加键盘事件监听
onMounted(() => {
  document.addEventListener("keydown", handleKeydown);
});

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener("keydown", handleKeydown);
  // 确保恢复页面滚动
  document.body.style.overflow = "";
});
</script>

<style scoped>
/* Viewer.js 风格的视频预览器 */
.video-viewer-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.95);
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
.video-viewer-toolbar {
  position: relative;
  height: 60px;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  padding: 0 20px;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.video-viewer-title {
  flex: 1;
  color: white;
  font-size: 16px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 20px;
}

.video-viewer-counter {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-right: 20px;
  font-family: monospace;
}

.video-viewer-close {
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

.video-viewer-close:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 主画布区域 */
.video-viewer-canvas {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 0;
}

/* 导航按钮 */
.video-viewer-nav {
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

.video-viewer-nav:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-50%) scale(1.1);
}

.video-viewer-prev {
  left: 30px;
}

.video-viewer-next {
  right: 30px;
}

/* 视频容器 */
.video-viewer-video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.video-viewer-video {
  max-width: calc(100% - 40px);
  max-height: calc(100% - 40px);
  width: auto;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  background: #000;
}

/* 当没有缩略图时，视频可以使用更多空间 */
.video-viewer-video-wrapper.no-thumbnails .video-viewer-video {
  max-height: calc(100vh - 60px - 40px);
}

/* 确保视频在有缩略图时也能正确显示 */
.video-viewer-video-wrapper:not(.no-thumbnails) .video-viewer-video {
  max-height: calc(100vh - 60px - 100px - 40px);
}

/* 缩略图区域 */
.video-viewer-thumbnails {
  height: 100px;
  background: rgba(0, 0, 0, 0.8);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 15px 20px;
}

.video-viewer-thumbnail-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  height: 70px;
  align-items: center;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;
}

.video-viewer-thumbnail-list::-webkit-scrollbar {
  height: 4px;
}

.video-viewer-thumbnail-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.video-viewer-thumbnail-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.video-viewer-thumbnail {
  flex-shrink: 0;
  width: 70px;
  height: 70px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  position: relative;
  background: #000;
}

.video-viewer-thumbnail:hover {
  border-color: rgba(255, 255, 255, 0.5);
}

.video-viewer-thumbnail.active {
  border-color: #007bff;
  box-shadow: 0 0 0 1px #007bff;
}

.video-viewer-thumbnail-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-viewer-toolbar {
    height: 50px;
    padding: 0 15px;
  }

  .video-viewer-title {
    font-size: 14px;
  }

  .video-viewer-counter {
    font-size: 12px;
    margin-right: 15px;
  }

  .video-viewer-close {
    width: 36px;
    height: 36px;
  }

  .video-viewer-nav {
    width: 44px;
    height: 44px;
  }

  .video-viewer-prev {
    left: 20px;
  }

  .video-viewer-next {
    right: 20px;
  }

  .video-viewer-thumbnails {
    height: 80px;
    padding: 10px 15px;
  }

  .video-viewer-thumbnail-list {
    height: 60px;
    gap: 8px;
  }

  .video-viewer-thumbnail {
    width: 60px;
    height: 60px;
  }

  .video-viewer-video-wrapper {
    padding: 15px;
  }

  .video-viewer-video {
    max-width: calc(100% - 30px);
    max-height: calc(100% - 30px);
  }

  .video-viewer-video-wrapper.no-thumbnails .video-viewer-video {
    max-height: calc(100vh - 50px - 30px);
  }

  .video-viewer-video-wrapper:not(.no-thumbnails) .video-viewer-video {
    max-height: calc(100vh - 50px - 80px - 30px);
  }

  .video-play-icon {
    width: 20px;
    height: 20px;
  }
}

@media (max-width: 480px) {
  .video-viewer-toolbar {
    height: 45px;
    padding: 0 10px;
  }

  .video-viewer-title {
    font-size: 13px;
    margin-right: 10px;
  }

  .video-viewer-counter {
    font-size: 11px;
    margin-right: 10px;
  }

  .video-viewer-close {
    width: 32px;
    height: 32px;
  }

  .video-viewer-nav {
    width: 40px;
    height: 40px;
  }

  .video-viewer-prev {
    left: 15px;
  }

  .video-viewer-next {
    right: 15px;
  }

  .video-viewer-thumbnails {
    height: 70px;
    padding: 8px 10px;
  }

  .video-viewer-thumbnail-list {
    height: 54px;
    gap: 6px;
  }

  .video-viewer-thumbnail {
    width: 54px;
    height: 54px;
  }

  .video-viewer-video-wrapper {
    padding: 10px;
  }

  .video-viewer-video {
    max-width: calc(100% - 20px);
    max-height: calc(100% - 20px);
  }

  .video-viewer-video-wrapper.no-thumbnails .video-viewer-video {
    max-height: calc(100vh - 45px - 20px);
  }

  .video-viewer-video-wrapper:not(.no-thumbnails) .video-viewer-video {
    max-height: calc(100vh - 45px - 70px - 20px);
  }

  .video-play-icon {
    width: 18px;
    height: 18px;
  }
}
</style>
