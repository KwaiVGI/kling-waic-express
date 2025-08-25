<script setup lang="ts">
import ImageComponent from './image.vue'
import VideoComponent from './video.vue'
import PrintComponent from './print.vue'
import ConfigComponent from './config.vue'
import LogoutButton from '@/components/LogoutButton.vue'

const tabs = [
  { value: 'image', label: '图片', component: ImageComponent },
  { value: 'video', label: '视频', component: VideoComponent },
  { value: 'print', label: '打印队列', component: PrintComponent },
  { value: 'config', label: '系统配置', component: ConfigComponent },
]
</script>

<template>
  <div class="control-view">
    <LogoutButton />
    <div class="header">
      <h1>活动控制后台</h1>
      <div class="feature-tips">
        📌 固定：大屏持续展示 • 🗑️ 删除：移出队列 • 🖨️ 打印：协助现场打印
      </div>
    </div>

    <div class="control-panel">
      <Tab :tabs="tabs" default-active="image" />
    </div>
  </div>
</template>

<style scoped>
.control-view {
  min-height: 100vh;
  padding: 40px 20px;
  margin: 0 auto;
  background: linear-gradient(135deg, #1a2a6c, #2a1a6c);
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 16px;
  background: linear-gradient(to right, #4361ee, #7209b7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header p {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.8);
  max-width: 800px;
  margin: 0 auto;
}

.feature-tips {
  max-width: 800px;
  margin: 16px auto 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
  line-height: 1.3;
}

.control-panel {
  border-radius: 20px;
  backdrop-filter: blur(10px);
}
</style>
