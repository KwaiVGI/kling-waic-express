<template>
  <div class="video-wall-container">
    <div class="configuration-bar">
      <div class="config-item">
        <label>网格尺寸:</label>
        <input
          type="number"
          v-model.number="gridSize"
          min="5"
          max="20"
          @change="updateGridSize"
        />
      </div>
      <div class="config-item">
        <label>轮询间隔(秒):</label>
        <input
          type="number"
          v-model.number="pollIntervalSeconds"
          min="1"
          max="60"
        />
      </div>
      <button @click="applyConfig" class="apply-btn">应用配置</button>
    </div>

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

    <div class="status-bar">
      <span>最后更新: {{ lastUpdateTime }}</span>
      <span>网格尺寸: {{ gridSize }}×{{ gridSize }}</span>
      <span>轮询间隔: {{ pollIntervalSeconds }}秒</span>
      <span>新视频待替换: {{ newVideosPending }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from "vue";

interface VideoItem {
  url: string;
  id: string;
  flipping: boolean;
}

// 配置参数
const gridSize = ref(10); // 默认10×10网格
const pollIntervalSeconds = ref(10); // 默认10秒轮询间隔
const pollInterval = ref<NodeJS.Timeout | null>(null);

// 视频数据
const displayedVideos = ref<VideoItem[]>([]);
const lastUpdateTime = ref("");
const newVideosPending = ref(0);
const totalVideos = computed(() => gridSize.value * gridSize.value);

// 真实可用的测试视频URL列表（100个不同的视频）
const REAL_VIDEO_URLS = Array(100)
  .fill(null)
  .map((_, i) => {
    // 使用不同的视频源确保多样性
    const sources = [
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
      "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
      "https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4",
      "https://assets.mixkit.co/videos/preview/mixkit-clouds-and-blue-sky-1170-large.mp4",
      "https://assets.mixkit.co/videos/preview/mixkit-going-down-a-curved-highway-down-a-mountain-41576-large.mp4",
      "https://assets.mixkit.co/videos/preview/mixkit-woman-walking-on-a-street-at-night-3982-large.mp4",
    ];

    // 每个视频添加唯一参数确保URL不同
    const source = sources[i % sources.length];
    return `${source}?v=${i}&t=${Date.now()}`;
  });

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

// 应用配置
const applyConfig = () => {
  // 清除旧的轮询
  if (pollInterval.value) {
    clearInterval(pollInterval.value);
  }

  // 重新初始化网格
  initVideoSlots();

  // 开始新的轮询
  startPolling();
};

// 更新网格大小
const updateGridSize = () => {
  // 确保网格尺寸在合理范围内
  if (gridSize.value < 5) gridSize.value = 5;
  if (gridSize.value > 20) gridSize.value = 20;
  applyConfig();
};

// 获取视频数据
const fetchVideos = async () => {
  try {
    // 模拟API调用 - 实际项目中替换为真实API
    // const response = await fetch(`/castings/video/${totalVideos.value}`)
    // const videos = await response.json()

    // 使用Mock数据
    const mockVideos = generateMockVideos(totalVideos.value);

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

// 生成Mock视频数据
const generateMockVideos = (count: number) => {
  const mockVideos = [];

  for (let i = 0; i < count; i++) {
    // 随机选择一个视频URL
    const randomIndex = Math.floor(Math.random() * REAL_VIDEO_URLS.length);
    const url = REAL_VIDEO_URLS[randomIndex];

    mockVideos.push({
      id: `video-${Date.now()}-${i}`,
      url: url,
    });
  }

  return mockVideos;
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
  width: 100vw;
  height: calc(177.78vw - 40px); /* 9:16 比例 */
  max-height: calc(100vh - 40px);
  max-width: calc(56.25vh - 40px); /* 保持比例 */
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
  gap: 1px;
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
  animation: flip 1s ease-in-out;
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

/* 响应式调整 */
@media (orientation: portrait) {
  .video-wall-container {
    height: calc(177.78vw - 40px);
    width: 100vw;
  }
}

@media (orientation: landscape) {
  .video-wall-container {
    width: calc(56.25vh - 40px);
    height: calc(100vh - 40px);
  }
}
</style>
