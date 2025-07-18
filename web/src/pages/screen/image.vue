<template>
  <div class="display-screen">
    <div v-if="currentImage" class="image-container">
      <div
        class="casting-image"
        :style="{ backgroundImage: `url(${currentImage.url})` }"
      ></div>
    </div>

    <div v-else class="no-image">
      <div class="no-image-content">
        <i class="fas fa-image"></i>
        <h3>等待图片数据...</h3>
        <p>系统正在获取展示内容</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { castingService, type CastingImage } from "@/api/castingService";

const currentImage = ref<CastingImage | null>(null);
const lastUpdateTime = ref("");
const isPolling = ref(true);
const pollingInterval = ref<NodeJS.Timeout | null>(null);
const pollingStatus = ref("正在获取图片...");

// 获取当前展示图片
const fetchCurrentCasting = async () => {
  try {
    const result = await castingService.getCurrentCasting();
    if (result.length > 0) {
      currentImage.value = result[0];
      lastUpdateTime.value = new Date().toLocaleString();
      pollingStatus.value = `下次更新: `;
    }
  } catch (error) {
    console.error("获取展示图片失败:", error);
    pollingStatus.value = "获取失败，5秒后重试...";
  }
};

// 开始轮询
const startPolling = () => {
  fetchCurrentCasting();
  pollingInterval.value = setInterval(fetchCurrentCasting, 5000);
};

onMounted(() => {
  startPolling();
});

onUnmounted(() => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value);
  }
});
</script>

<style scoped>
.display-screen {
  height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  background-color: #000;
}

.status-bar {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: 15px 25px;
  z-index: 100;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(5px);
}

.mode-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mode-tag {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.mode-tag.carousel {
  background: rgba(67, 97, 238, 0.8);
}

.mode-tag.fixed {
  background: rgba(114, 9, 183, 0.8);
}

.image-title {
  font-size: 16px;
}

.last-update {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.image-container {
  height: 100%;
  width: 100%;
}

.casting-image {
  height: 100%;
  width: 100%;
  background-size: auto 100%;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 50%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  z-index: 1;
}

.image-info {
  position: relative;
  z-index: 2;
  padding: 40px;
  max-width: 900px;
  color: white;
}

.image-info h2 {
  font-size: 3rem;
  margin-bottom: 20px;
}

.image-info p {
  font-size: 1.4rem;
  opacity: 0.9;
  line-height: 1.6;
}

.no-image {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.no-image-content {
  max-width: 600px;
  padding: 40px;
}

.no-image-content i {
  font-size: 5rem;
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.2);
}

.no-image-content h3 {
  font-size: 2rem;
  margin-bottom: 16px;
}

.no-image-content p {
  font-size: 1.2rem;
  opacity: 0.8;
}

.polling-indicator {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 100;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  border-radius: 30px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-link {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 100;
  padding: 10px 20px;
  background: rgba(67, 97, 238, 0.8);
  border-radius: 30px;
  color: white;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.control-link:hover {
  background: rgba(86, 113, 240, 0.9);
  transform: translateY(-3px);
}
</style>
