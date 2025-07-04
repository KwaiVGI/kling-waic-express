<template>
  <div class="control-view">
    <div class="header">
      <h1>大屏图片轮播控制系统</h1>
    </div>

    <div class="control-panel">
      <Image></Image>
    </div>
  </div>
</template>

<script setup lang="ts">
import Image from "./image.vue";
</script>

<style scoped>
.control-view {
  min-height: 100vh;
  padding: 40px 20px;
  max-width: 1600px;
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

.control-panel {
  border-radius: 20px;
  backdrop-filter: blur(10px);
}
</style>
