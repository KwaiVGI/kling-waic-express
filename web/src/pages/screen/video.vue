<template>
  <div class="video-wall-container">
    <div class="video-grid">
      <div
        v-for="(cell, index) in videoCells"
        :key="`cell-${index}-${cell.current?.id || 'empty'}`"
        class="video-cell"
      >
        <!-- 当前播放的视频 -->
        <video
          v-if="cell.current && cell.current.ready"
          ref="currentVideos"
          :data-index="index"
          :src="cell.current.url"
          :poster="cell.current.poster"
          autoplay
          muted
          playsinline
          class="video-element active"
          @ended="onVideoEnded(index)"
          @error="handleVideoError(index)"
        ></video>

        <!-- 预加载的下一个视频（已加载完成但隐藏） -->
        <video
          v-if="cell.next && cell.next.ready"
          ref="nextVideos"
          :data-index="index"
          :src="cell.next.url"
          muted
          playsinline
          class="video-element next"
        ></video>

        <!-- 过渡遮罩 -->
        <!-- <div
          v-if="cell.transitioning"
          class="transition-overlay"
          @animationend="cell.transitioning = false"
        ></div> -->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { castingService } from "@/api/castingService";
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";
import { ref, onMounted, onUnmounted, shallowRef, nextTick } from "vue";

interface VideoItem {
  poster: string;
  url: string;
  id: string;
  ready: boolean;
  element?: HTMLVideoElement;
}

interface VideoCell {
  current: VideoItem | null;
  next: VideoItem | null;
  transitioning: boolean;
}

// 配置参数
const gridSize = ref(3); // 默认3x3网格

// 视频格子数据
const videoCells = ref<VideoCell[]>([]);
const currentVideos = shallowRef<HTMLVideoElement[]>([]);
const nextVideos = shallowRef<HTMLVideoElement[]>([]);
const preloadLock = ref(false); // 预加载锁防止重复请求

// 初始化视频格子
const initVideoCells = async () => {
  const total = gridSize.value * gridSize.value;
  videoCells.value = Array(total)
    .fill(null)
    .map(() => ({
      current: null,
      next: null,
      transitioning: false,
    }));

  // 批量加载初始视频
  await preloadInitialVideos();

  // 预加载下一批视频
  schedulePreload();
};

// 预加载初始视频
const preloadInitialVideos = async () => {
  const total = gridSize.value * gridSize.value;
  const videos = await castingService.getCurrentCasting("VIDEO_EFFECT", total);

  await Promise.all(
    videos.map(async (video, index) => {
      if (index < total) {
        const videoItem = { ...video, ready: false };
        videoCells.value[index].current = videoItem;
        await preloadVideo(videoItem);
      }
    })
  );
};

// 预加载单个视频并确认完成
const preloadVideo = (videoItem: VideoItem): Promise<void> => {
  return new Promise((resolve) => {
    const videoEl = document.createElement("video");
    videoEl.src = videoItem.url;
    videoEl.muted = true;
    videoEl.preload = "auto";

    const onCanPlay = () => {
      cleanup();
      videoItem.ready = true;
      videoItem.element = videoEl;
      resolve();
    };

    const onError = () => {
      console.error(`视频加载失败: ${videoItem.url}`);
      cleanup();
      resolve();
    };

    const cleanup = () => {
      videoEl.removeEventListener("canplay", onCanPlay);
      videoEl.removeEventListener("error", onError);
      videoEl.remove();
    };

    videoEl.addEventListener("canplay", onCanPlay, { once: true });
    videoEl.addEventListener("error", onError, { once: true });

    videoEl.load();
  });
};

// 预加载下一批视频（智能调度）
const schedulePreload = () => {
  if (preloadLock.value) return;

  // 计算需要预加载的数量
  const emptySlots = videoCells.value.filter((cell) => !cell.next).length;
  if (emptySlots === 0) return;

  preloadLock.value = true;
  const preloadCount = Math.min(emptySlots, gridSize.value * gridSize.value);

  castingService
    .getCurrentCasting("VIDEO_EFFECT", preloadCount)
    .then(async (videos) => {
      const preloadTasks = [];

      // 优先分配给没有预加载视频的格子
      for (let i = 0; i < videos.length; i++) {
        const cellIndex = videoCells.value.findIndex((cell) => !cell.next);
        if (cellIndex === -1) break;

        const videoItem = { ...videos[i], ready: false };
        videoCells.value[cellIndex].next = videoItem;
        preloadTasks.push(preloadVideo(videoItem));
      }

      await Promise.all(preloadTasks);
      console.log(`预加载完成: ${preloadCount}个视频`);
    })
    .catch((error) => {
      console.error("预加载请求失败:", error);
    })
    .finally(() => {
      preloadLock.value = false;
    });
};

// 处理视频播放结束
const onVideoEnded = (index: number) => {
  const cell = videoCells.value[index];

  if (cell.next?.ready) {
    // 触发切换动画
    cell.transitioning = true;

    setTimeout(() => {
      // 切换到预加载的视频
      cell.current = cell.next;
      cell.next = null;

      // 触发新的预加载
      schedulePreload();

      // 确保新视频播放
      nextTick(() => {
        const videoEl = currentVideos.value.find(
          (el) => parseInt(el.dataset.index || "0") === index
        );
        if (videoEl) {
          videoEl.play().catch((e) => console.error("视频播放失败:", e));
        }
      });
    }, 300); // 匹配动画时间
  } else {
    // 没有准备好的下一个视频，重新播放当前视频
    const videoEl = currentVideos.value.find(
      (el) => parseInt(el.dataset.index || "0") === index
    );
    if (videoEl && cell.current?.ready) {
      videoEl.currentTime = 0;
      videoEl.play().catch((e) => console.error("视频重播失败:", e));
    } else {
      // 当前视频也不可用，触发紧急加载
      reloadVideo(index);
    }

    // 触发预加载补充
    schedulePreload();
  }
};

// 重新加载单个视频
const reloadVideo = async (index: number) => {
  try {
    const [video] = await castingService.getCurrentCasting("VIDEO_EFFECT", 1);
    const videoItem = { ...video, ready: false };
    videoCells.value[index].current = videoItem;
    await preloadVideo(videoItem);
  } catch (e) {
    console.error(`加载视频失败: ${index}`, e);
  }
};

// 处理视频错误
const handleVideoError = (index: number) => {
  console.error(`视频播放错误: ${index}`);
  reloadVideo(index);
};

const route = useRoute();
// 初始化加载
onMounted(() => {
  if (route.query.token) {
    localStorage.setItem(STORAGE_TOKEN_KEY, route.query.token as string);
  }
  initVideoCells();
});

// 清理
onUnmounted(() => {
  videoCells.value.forEach((cell) => {
    if (cell.current?.element) {
      cell.current.element.pause();
      cell.current.element.removeAttribute("src");
    }
    if (cell.next?.element) {
      cell.next.element.pause();
      cell.next.element.removeAttribute("src");
    }
  });
});
</script>

<style scoped>
.video-wall-container {
  height: 100vh;
  width: calc(100vh * 9 / 16); /* 9:16 竖屏比例 */
  margin: 0 auto;
  overflow: hidden;
  background-color: #000;
  position: relative;
  display: flex;
  flex-direction: column;
}

.video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(v-bind("gridSize"), 1fr);
  grid-template-rows: repeat(v-bind("gridSize"), 1fr);
  width: 100%;
  height: 100%;
  gap: 0;
  box-sizing: border-box;
}

.video-cell {
  position: relative;
  background-color: #111;
  overflow: hidden;
  border-radius: 0;
}

.video-element {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.3s ease-in-out;
}

.video-element.next {
  opacity: 0;
  z-index: 1;
}

.video-element.active {
  opacity: 1;
  z-index: 2;
}

.transition-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: white;
  z-index: 10;
  opacity: 0;
  animation: fadeTransition 0.3s ease-in-out;
}

@keyframes fadeTransition {
  0% {
    opacity: 0;
  }
  50% {
    opacity: 0.8;
  }
  100% {
    opacity: 0;
  }
}
</style>
