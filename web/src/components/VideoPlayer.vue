<script setup>
import { onMounted, ref } from 'vue'

const props = defineProps({
  src: {
    type: String,
    required: true,
  },
  poster: {
    type: String,
    default: '',
  },
  autoplay: {
    type: Boolean,
    default: false,
  },
})
const fullIcon
  = 'https://ali.a.yximgs.com/kos/nlav12119/UEoNaVQo_2025-07-21-15-32-15.png'
const exitFullIcon
  = 'https://ali.a.yximgs.com/kos/nlav12119/ubeqgmzS_2025-07-21-16-04-39.png'
const playerRef = ref(null)
const videoRef = ref(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const isLoading = ref(true)
const showOverlay = ref(true)
const controlsVisible = ref(true)
let controlsTimeout = null
const isFullscreen = ref(false)

// 格式化时间显示
function formatTime(time) {
  const minutes = Math.floor(time / 60)
  const seconds = Math.floor(time % 60)
  return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`
}

// 播放/暂停
function togglePlay() {
  if (isPlaying.value) {
    videoRef.value.pause()
    showOverlay.value = true
  }
  else {
    videoRef.value.play()
    showOverlay.value = false
  }
  showControls()
}

// 更新播放进度
function updateProgress() {
  currentTime.value = videoRef.value.currentTime
}

// 设置播放进度
function setProgress(e) {
  const progressBar = e.currentTarget
  const rect = progressBar.getBoundingClientRect()
  const pos = (e.clientX - rect.left) / rect.width
  videoRef.value.currentTime = pos * videoRef.value.duration
  showControls()
}

// 显示控制条
function showControls() {
  controlsVisible.value = true
  clearTimeout(controlsTimeout)
  controlsTimeout = setTimeout(() => {
    controlsVisible.value = false
  }, 3000)
}

const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream
function enterIOSFullscreen() {
  const video = videoRef.value
  if (video.webkitEnterFullscreen) {
    video.webkitEnterFullscreen()
  }
  else if (video.webkitRequestFullscreen) {
    video.webkitRequestFullscreen()
  }
}

// 切换全屏
function toggleFullscreen() {
  if (!isFullscreen.value) {
    const player = playerRef.value
    if (isIOS) {
      // iOS特殊处理
      enterIOSFullscreen()
    }
    else if (player.requestFullscreen) {
      player.requestFullscreen()
    }
    else if (player.webkitRequestFullscreen) {
      player.webkitRequestFullscreen() // Safari
    }
    else if (player.msRequestFullscreen) {
      player.msRequestFullscreen()
    }
    else if (player.webkitEnterFullscreen) {
      // iOS Safari的特殊处理
      player.webkitEnterFullscreen()
    }
  }
  else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    }
    else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen()
    }
    else if (document.msExitFullscreen) {
      document.msExitFullscreen()
    }
  }
}

// 处理全屏变化
function handleFullscreenChange() {
  isFullscreen.value = !!(
    document.fullscreenElement
    || document.webkitFullscreenElement
    || document.msFullscreenElement
  )
}

// 初始化事件监听
function initEvents() {
  videoRef.value.addEventListener('timeupdate', updateProgress)
  videoRef.value.addEventListener('loadedmetadata', () => {
    duration.value = videoRef.value.duration
    isLoading.value = false
  })
  videoRef.value.addEventListener('canplay', () => {
    isLoading.value = false
  })
  videoRef.value.addEventListener('waiting', () => {
    isLoading.value = true
  })
  videoRef.value.addEventListener('playing', () => {
    isLoading.value = false
    isPlaying.value = true
    showOverlay.value = false
  })
  videoRef.value.addEventListener('pause', () => {
    isPlaying.value = false
    showOverlay.value = true
  })
  videoRef.value.addEventListener('ended', () => {
    isPlaying.value = false
    showOverlay.value = true
  })

  // 添加点击显示控制条
  videoRef.value.addEventListener('click', () => {
    togglePlay()
  })

  // 添加触摸事件显示控制条
  videoRef.value.addEventListener('touchstart', () => {
    showControls()
  })

  // 全屏事件监听
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('msfullscreenchange', handleFullscreenChange)
}

onMounted(() => {
  initEvents()
  // 初始显示控制条，3秒后隐藏
  controlsTimeout = setTimeout(() => {
    controlsVisible.value = false
  }, 3000)
})
onUnmounted(() => {
  // 清理事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener(
    'webkitfullscreenchange',
    handleFullscreenChange,
  )
  document.removeEventListener('msfullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <div ref="playerRef" class="video-player">
    <video
      ref="videoRef"
      :src="src"
      :poster="poster"

      x5-playsinline webkit-playsinline playsinline
      preload="metadata"
    />

    <div
      class="play-overlay"
      :class="{ visible: showOverlay }"
      @click="togglePlay"
    >
      <div class="play-icon">
        <img
          v-if="isPlaying"
          class="h-24px w-24px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ULkRxVvc_2025-07-21-15-26-51.png"
          alt=""
        >
        <img
          v-else
          class="h-24px w-24px"
          src="https://ali.a.yximgs.com/kos/nlav12119/ZLdLThMO_2025-07-21-12-35-13.png"
          alt=""
        >
      </div>
    </div>

    <div v-show="isLoading" class="loading-indicator">
      <div class="i-carbon-circle-dash animate-spin" />
    </div>

    <div class="controls">
      <div
        class="controls-top px-12px flex items-center box-border justify-between"
      >
        <div class="time-display">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </div>
        <div
          class="flex h-20px w-20px items-center justify-center"
          @click="toggleFullscreen"
        >
          <img
            class="h-full w-full"
            :src="isFullscreen ? exitFullIcon : fullIcon"
            alt=""
          >
        </div>
      </div>

      <div class="progress-container" @click="setProgress">
        <div
          class="progress-bar"
          :style="{ width: `${(currentTime / duration) * 100}%` }"
        />
      </div>
    </div>
  </div>
</template>

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
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  height: 4px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 2px;
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
  background: rgba(0, 0, 0, 0.1);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 10;
}

.play-overlay.visible {
  opacity: 1;
}

.play-icon {
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.4);
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
