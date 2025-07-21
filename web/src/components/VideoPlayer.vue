<template>
  <div class="video-player">
    <video
      ref="videoRef"
      :src="src"
      :poster="poster"
      playsinline
      webkit-playsinline
      x5-playsinline
      preload="metadata"
    ></video>

    <div
      class="play-overlay"
      :class="{ visible: showOverlay }"
      @click="togglePlay"
    >
      <div class="play-icon">
        <img
          v-if="isPlaying"
          class="w-24px h-24px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ZLdLThMO_2025-07-21-12-35-13.png"
          alt=""
        />
        <img
          v-else
          class="w-24px h-24px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ZLdLThMO_2025-07-21-12-35-13.png"
          alt=""
        />
      </div>
    </div>

    <div class="loading-indicator" v-show="isLoading">
      <i class="fas fa-spinner fa-spin"></i>
    </div>

    <div class="controls" v-show="controlsVisible">
      <!-- <button class="control-btn" @click="togglePlay">
        <img
          v-if="isPlaying"
          class="w-20px h-20px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ZLdLThMO_2025-07-21-12-35-13.png"
          alt=""
        />
        <img
          v-else
          class="w-20px h-20px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ZLdLThMO_2025-07-21-12-35-13.png"
          alt=""
        />
      </button> -->

      <div class="time-display">
        {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
      </div>

      <div class="progress-container" @click="setProgress">
        <div
          class="progress-bar"
          :style="{ width: (currentTime / duration) * 100 + '%' }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";

const props = defineProps({
  src: {
    type: String,
    required: true,
  },
  poster: {
    type: String,
    default: "",
  },
});

const videoRef = ref(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const isLoading = ref(true);
const showOverlay = ref(true);
const controlsVisible = ref(true);
let controlsTimeout = null;

// 格式化时间显示
const formatTime = (time) => {
  const minutes = Math.floor(time / 60);
  const seconds = Math.floor(time % 60);
  return `${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;
};

// 播放/暂停
const togglePlay = () => {
  if (isPlaying.value) {
    videoRef.value.pause();
    showOverlay.value = true;
  } else {
    videoRef.value.play();
    showOverlay.value = false;
  }
  showControls();
};

// 更新播放进度
const updateProgress = () => {
  currentTime.value = videoRef.value.currentTime;
};

// 设置播放进度
const setProgress = (e) => {
  const progressBar = e.currentTarget;
  const rect = progressBar.getBoundingClientRect();
  const pos = (e.clientX - rect.left) / rect.width;
  videoRef.value.currentTime = pos * videoRef.value.duration;
  showControls();
};

// 显示控制条
const showControls = () => {
  controlsVisible.value = true;
  clearTimeout(controlsTimeout);
  controlsTimeout = setTimeout(() => {
    controlsVisible.value = false;
  }, 3000);
};

// 初始化事件监听
const initEvents = () => {
  videoRef.value.addEventListener("timeupdate", updateProgress);
  videoRef.value.addEventListener("loadedmetadata", () => {
    duration.value = videoRef.value.duration;
    isLoading.value = false;
  });
  videoRef.value.addEventListener("canplay", () => {
    isLoading.value = false;
  });
  videoRef.value.addEventListener("waiting", () => {
    isLoading.value = true;
  });
  videoRef.value.addEventListener("playing", () => {
    isLoading.value = false;
    isPlaying.value = true;
    showOverlay.value = false;
  });
  videoRef.value.addEventListener("pause", () => {
    isPlaying.value = false;
    showOverlay.value = true;
  });
  videoRef.value.addEventListener("ended", () => {
    isPlaying.value = false;
    showOverlay.value = true;
  });

  // 添加点击显示控制条
  videoRef.value.addEventListener("click", () => {
    togglePlay();
  });

  // 添加触摸事件显示控制条
  videoRef.value.addEventListener("touchstart", () => {
    showControls();
  });
};

onMounted(() => {
  initEvents();
  // 初始显示控制条，3秒后隐藏
  controlsTimeout = setTimeout(() => {
    controlsVisible.value = false;
  }, 3000);
});
</script>

<style scoped lang="scss">
.video-player {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 Aspect Ratio */
}

.video-player video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #000;
  display: block;
  cursor: pointer;
}

.controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.75), transparent);
  padding: 12px 10px;
  display: flex;
  align-items: center;
  transition: opacity 0.3s;
  z-index: 20;
}

.control-btn {
  background: none;
  border: none;
  color: #fff;
  font-size: 1.1rem;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
  flex-shrink: 0;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.progress-container {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 2px;
  margin: 0 12px;
  cursor: pointer;
  position: relative;
  flex-shrink: 1;
}

.progress-bar {
  height: 100%;
  background: #fff;
  border-radius: 2px;
  width: 0%;
  position: relative;
  transition: width 0.2s;
}

.time-display {
  color: #fff;
  font-size: 0.85rem;
  min-width: 75px;
  text-align: center;
  flex-shrink: 0;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 10;
}

.play-overlay.visible {
  opacity: 1;
}

.play-icon {
  width: 65px;
  height: 65px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  color: #fff;
  transition: all 0.3s;
}

.play-overlay:hover .play-icon {
  transform: scale(1.1);
  background: rgba($color: #000, $alpha: 0.8);
}

.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 2.2rem;
  z-index: 5;
}

@media (max-width: 600px) {
  .control-btn {
    font-size: 1rem;
    width: 32px;
    height: 32px;
  }

  .time-display {
    min-width: 70px;
    font-size: 0.8rem;
  }

  .play-icon {
    width: 55px;
    height: 55px;
    font-size: 1.6rem;
  }
}
</style>
