<template>
  <div class="video-wall-container">
    <div class="video-grid">
      <div
        v-for="(video, index) in displayedVideos"
        :key="index"
        class="video-cell"
        :class="{ flipping: video.flipping }"
        @animationend="handleFlipEnd(index)"
      >
        <video
          v-if="video.url"
          :src="video.url"
          autoplay
          loop
          muted
          playsinline
          class="video-element"
        ></video>
        <div v-else class="video-placeholder">
          <span class="placeholder-text">视频加载中...</span>
        </div>
      </div>
    </div>

    <!-- <div class="status-bar">
      <span>最后更新: {{ lastUpdateTime }}</span>
      <span>网格尺寸: {{ gridSize }}×{{ gridSize }}</span>
      <span>轮询间隔: {{ pollIntervalSeconds }}秒</span>
      <span>新视频待替换: {{ newVideosPending }}</span>
    </div> -->
  </div>
</template>

<script setup lang="ts">
import { castingService } from "@/api/castingService";
import { ref, onMounted, onUnmounted, computed, watch } from "vue";

interface VideoItem {
  url: string;
  id: string;
  flipping: boolean;
}

// 配置参数
const gridSize = ref(3); // 默认10×10网格
const pollIntervalSeconds = ref(10); // 默认10秒轮询间隔
const pollInterval = ref<NodeJS.Timeout | null>(null);

// 视频数据
const displayedVideos = ref<VideoItem[]>([]);
const lastUpdateTime = ref("");
const newVideosPending = ref(0);
const totalVideos = computed(() => gridSize.value * gridSize.value);

// 获取当前显示的所有视频URL
const getCurrentVideoUrls = () => {
  const urls = new Set<string>();
  displayedVideos.value.forEach((video) => {
    if (video.url) urls.add(video.url.split("?")[0]); // 忽略查询参数
  });
  return urls;
};

// 初始化视频槽位
const initVideoSlots = () => {
  const total = totalVideos.value;
  displayedVideos.value = Array(total)
    .fill(null)
    .map(() => ({
      url: "",
      id: "",
      flipping: false,
    }));
};

// 获取视频数据
const fetchVideos = async () => {
  try {
    const mockVideos = await castingService.getCurrentCasting(
      "VIDEO_EFFECT",
      gridSize.value * gridSize.value
    );

    // 更新最后更新时间
    lastUpdateTime.value = new Date().toLocaleTimeString();

    // 获取当前已显示的视频URL（忽略查询参数）
    const currentUrls = getCurrentVideoUrls();

    // 过滤出真正新的视频（当前界面没有的）
    const trulyNewVideos = mockVideos.filter((video) => {
      const baseUrl = video.url.split("?")[0];
      return !currentUrls.has(baseUrl);
    });

    newVideosPending.value = trulyNewVideos.length;

    if (trulyNewVideos.length > 0) {
      // 随机替换现有视频
      replaceRandomVideos(trulyNewVideos);
    }
  } catch (error) {
    console.error("获取视频失败:", error);
  }
};

// 随机替换视频
const replaceRandomVideos = (newVideos: VideoItem[]) => {
  // 找出当前显示的视频槽位（排除正在翻转的）
  const availableSlots = displayedVideos.value
    .map((_, index) => index)
    .filter((index) => !displayedVideos.value[index].flipping);

  // 随机打乱可用槽位
  const shuffledSlots = [...availableSlots].sort(() => Math.random() - 0.5);

  // 最多替换新视频数量的槽位
  const slotsToReplace = Math.min(newVideos.length, shuffledSlots.length);

  for (let i = 0; i < slotsToReplace; i++) {
    const slotIndex = shuffledSlots[i];
    const newVideo = newVideos[i];

    // 标记为正在翻转
    displayedVideos.value[slotIndex].flipping = true;

    // 短暂延迟后更新视频内容
    setTimeout(() => {
      displayedVideos.value[slotIndex] = {
        url: newVideo.url,
        id: newVideo.id,
        flipping: false,
      };
      newVideosPending.value--;
    }, 500); // 与CSS动画时间匹配
  }
};

// 处理翻转动画结束
const handleFlipEnd = (index: number) => {
  displayedVideos.value[index].flipping = false;
};

// 开始轮询
const startPolling = () => {
  fetchVideos(); // 立即获取一次
  pollInterval.value = setInterval(
    fetchVideos,
    pollIntervalSeconds.value * 1000
  );
};

// 初始化和启动
onMounted(() => {
  initVideoSlots();
  startPolling();
});

// 组件卸载时清理
onUnmounted(() => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value);
  }
});

// 监听轮询间隔变化
watch(pollIntervalSeconds, (newVal) => {
  if (newVal < 1) pollIntervalSeconds.value = 1;
  if (newVal > 60) pollIntervalSeconds.value = 60;
});
</script>

<style scoped>
.video-wall-container {
  height: 100vh;
  width: calc(100vh * 9 / 16);
  /* height: calc(177.78vw);  */
  /* 9:16 比例 */
  /* max-height: calc(100vh); */
  /* max-width: calc(56.25vh); 保持比例 */
  margin: 0 auto;
  overflow: hidden;
  background-color: #000;
  position: relative;
  display: flex;
  flex-direction: column;
}

.configuration-bar {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 10px;
  background-color: rgba(0, 0, 0, 0.8);
  z-index: 10;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
}

.config-item label {
  font-size: 14px;
}

.config-item input {
  width: 60px;
  padding: 5px 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #333;
  color: white;
}

.apply-btn {
  padding: 5px 15px;
  background-color: #4361ee;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.apply-btn:hover {
  background-color: #3a56d4;
}

.video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(v-bind(gridSize), 1fr);
  grid-template-rows: repeat(v-bind(gridSize), 1fr);
  width: 100%;
  height: 100%;
  gap: 0;
}

.video-cell {
  position: relative;
  background-color: #111;
  overflow: hidden;
  transform-style: preserve-3d;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #222;
}

.placeholder-text {
  color: #666;
  font-size: 12px;
}

/* 翻转动画 */
.video-cell.flipping {
  /* animation: flip 1s ease-in-out; */
}

@keyframes flip {
  0% {
    transform: rotateY(0deg);
  }
  50% {
    transform: rotateY(90deg);
    opacity: 0;
  }
  51% {
    opacity: 0;
  }
  100% {
    transform: rotateY(0deg);
    opacity: 1;
  }
}

.status-bar {
  display: flex;
  justify-content: space-around;
  padding: 8px;
  background-color: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 12px;
}
</style>
