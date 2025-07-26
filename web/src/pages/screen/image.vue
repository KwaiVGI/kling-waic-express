<template>
  <div class="display-screen">
    <!-- 双图片容器实现无缝切换 -->
    <div class="ds-image-container">
      <div class="absolute left-0 top-0 w-full z-50">
        <img
          class="w-full"
          src="https://ali.a.yximgs.com/kos/nlav12119/hquZnuZl_2025-07-24-16-50-11.png"
          alt=""
        />
      </div>
      <div
        v-if="pinedImage"
        class="ds-casting-image pined"
        :style="{ backgroundImage: `url(${pinedImage})` }"
      ></div>
      <template v-else>
        <div
          class="ds-casting-image active"
          :style="{
            backgroundImage: currentImage ? `url(${currentImage.url})` : '',
          }"
        ></div>
        <div
          class="ds-casting-image next"
          :style="{ backgroundImage: nextImage ? `url(${nextImage.url})` : '' }"
          :class="{ ready: nextImageLoaded }"
        ></div>
      </template>
    </div>
    <template v-if="!pinedImage">
      <!-- 加载状态提示 -->
      <div v-if="!currentImage && !loadingError" class="no-image">
        <div class="no-image-content">
          <i class="fas fa-image"></i>
          <h3>正在加载图片...</h3>
          <p>系统正在获取展示内容</p>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="loadingError" class="error-message">
        <div class="error-content">
          <i class="fas fa-exclamation-triangle"></i>
          <h3>图片加载失败</h3>
          <p>正在尝试重新连接... {{ retryCountdown }}秒</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { castingService, type CastingImage } from "@/api/castingService";
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";

// 图片队列管理
const imageQueue = ref<CastingImage[]>([]);
const currentImage = ref<CastingImage | null>(null);
const nextImage = ref<CastingImage | null>(null);
const nextImageLoaded = ref(false);
const transitionTimer = ref<NodeJS.Timeout | null>(null);
const preloadCount = 3; // 预加载图片数量
const loadingError = ref(false);
const retryTimer = ref<NodeJS.Timeout | null>(null);
const retryCountdown = ref(0);
const isFetching = ref(false);
const pinedImage = ref<string | null>(null);
const pinedImageCheckTimer = ref<NodeJS.Timeout | null>(null);

// 预加载图片资源
const preloadImage = (url: string): Promise<void> => {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve();
    img.onerror = () => resolve(); // 即使出错也继续
    img.src = url;
  });
};

const fetchPinedImage = async () => {
  try {
    const res = await castingService.getPinedImage();
    const url = res.task?.outputs?.url;
    if (res && url) {
      await preloadImage(url);
      pinedImage.value = url;
      // 暂停轮播
    } else {
      pinedImage.value = null;
      // 恢复轮播
    }
  } catch (error) {
    console.error("获取固定照片失败:", error);
    pinedImage.value = null;
  }
};

// 获取展示图片（带重试机制）
const fetchCastingImages = async () => {
  await fetchPinedImage();
  if (pinedImage.value) return;

  if (isFetching.value) return;
  isFetching.value = true;
  loadingError.value = false;

  try {
    const result = await castingService.getCurrentCasting(
      "STYLED_IMAGE",
      preloadCount
    );
    if (result.length > 0) {
      // 并行预加载所有图片
      await Promise.all(result.map((img) => preloadImage(img.url)));

      imageQueue.value = [...imageQueue.value, ...result];

      // 初始化当前图片
      if (!currentImage.value && imageQueue.value.length > 0) {
        currentImage.value = imageQueue.value.shift() || null;
      }

      // 初始化下一张图片
      if (!nextImage.value && imageQueue.value.length > 0) {
        nextImage.value = imageQueue.value.shift() || null;
        nextImageLoaded.value = true;
      }
    }
  } catch (error) {
    console.error("获取展示图片失败:", error);
    handleLoadingError();
  } finally {
    isFetching.value = false;
  }
};

// 处理加载错误
const handleLoadingError = () => {
  loadingError.value = true;
  retryCountdown.value = 5;

  if (retryTimer.value) clearInterval(retryTimer.value);
  retryTimer.value = setInterval(() => {
    retryCountdown.value--;
    if (retryCountdown.value <= 0) {
      clearInterval(retryTimer.value!);
      fetchCastingImages();
    }
  }, 1000);
};

// 切换到下一张图片
const transitionToNextImage = () => {
  if (pinedImage.value) return; // 有固定照片时不切换
  // 确保下一张图片已加载
  if (!nextImage.value || !nextImageLoaded.value) {
    console.warn("下一张图片尚未加载完成，跳过切换");
    return;
  }

  // 更新图片引用
  currentImage.value = nextImage.value;
  nextImage.value = imageQueue.value.shift() || null;
  nextImageLoaded.value = false;

  // 预加载新图片
  if (nextImage.value) {
    preloadImage(nextImage.value.url).then(() => {
      nextImageLoaded.value = true;
    });
  }

  // 当队列不足时补充新图片
  if (imageQueue.value.length < 2) {
    fetchCastingImages();
  }
};

// 安全启动轮播
const safeStartCarousel = () => {
  if (pinedImage.value) return; // 有固定照片时不启动
  if (transitionTimer.value) clearInterval(transitionTimer.value);

  transitionTimer.value = setInterval(() => {
    // 只在下一张图片加载完成时切换
    if (nextImageLoaded.value) {
      transitionToNextImage();
    } else {
      console.log("等待下一张图片加载...");
    }
  }, 5000);
};

// 监听图片变化
watch([currentImage, nextImage], ([cur, next]) => {
  if (cur && next) {
    safeStartCarousel();
  }
});
const route = useRoute();
// 初始化加载
onMounted(() => {
  if (route.query.token) {
    // localStorage.setItem(STORAGE_TOKEN_KEY, route.query.token as string);
  }
  fetchCastingImages();
  pinedImageCheckTimer.value = setInterval(fetchPinedImage, 1000);
});

// 清理资源
onUnmounted(() => {
  if (transitionTimer.value) clearInterval(transitionTimer.value);
  if (retryTimer.value) clearInterval(retryTimer.value);
  if (pinedImageCheckTimer.value) clearInterval(pinedImageCheckTimer.value);
});
</script>

<style scoped>
/* 基础全屏样式 */
.display-screen {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #000;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 9:16比例容器 */
.ds-image-container {
  position: relative;
  width: 100%;
  height: 100%;
  /* width: 100vw; 使用视口最小单位 */
  /* height: 100vh; 16/9 * 100vmin */
  /* max-width: 100%; */
  /* max-height: 100vh; */
  /* aspect-ratio: 9/16; 现代浏览器支持 */
  overflow: hidden;
}

/* 图片通用样式 */
.ds-casting-image {
  position: absolute;
  bottom: 2vh;
  /* 适配现场大屏 */
  left: 0.5%;
  width: 100vw;
  height: calc(100vw * 3 / 2);
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  transition: opacity 1.5s ease-in-out;
  will-change: opacity; /* 启用硬件加速 */
  opacity: 0;
  &.pined {
    opacity: 1;
    z-index: 3;
  }
}

/* 当前图片 */
.ds-casting-image.active {
  opacity: 1;
  z-index: 2;
}

/* 下一张图片 */
.ds-casting-image.next {
  opacity: 0;
  z-index: 1;
}

/* 当下一张图片加载完成时 */
.ds-casting-image.next.ready {
  opacity: 0; /* 保持不可见直到切换 */
}

/* 切换动画 */
.ds-casting-image.active {
  animation: fadeIn 1.5s;
}

@keyframes fadeIn {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

/* 无图状态和错误状态 */
.no-image,
.error-message {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  z-index: 10;
  background-color: rgba(0, 0, 0, 0.7);
}

.no-image-content,
.error-content {
  max-width: 600px;
  padding: 40px;
  color: #fff;
}

.error-content {
  color: #ff6b6b;
}

.no-image-content i,
.error-content i {
  font-size: 5rem;
  margin-bottom: 20px;
}

.no-image-content h3,
.error-content h3 {
  font-size: 2rem;
  margin-bottom: 16px;
}

.no-image-content p,
.error-content p {
  font-size: 1.2rem;
  opacity: 0.8;
}

/* 状态指示器 */
.status-indicator {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 8px 15px;
  border-radius: 20px;
  font-size: 0.9rem;
  z-index: 20;
}
</style>
