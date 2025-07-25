<template>
  <div class="control-view">
    <div class="header">
      <h1>WAIC 大屏控制系统</h1>
      <div class="text-12px text-left text-yellow-500">
        固定：将某张图片固定在大屏上一直展示直到被另一张固定的图片冲掉或被取消固定<br />
        置顶：将某张图片放到队头展示，不影响轮播，后置顶的在先置顶的前面<br />
        删除：不会大屏上展示，已经在展示的不受影响<br />
        打印：协助现场用户打印照片
      </div>
    </div>

    <div class="control-panel">
      <Image></Image>
      <Tab :tabs="tabs" defaultActive="image" />
    </div>
  </div>
</template>

<script setup lang="ts">
import ImageComponent from "./image.vue";
import VideoComponent from "./video.vue";
import PrintComponent from "./print.vue";
const tabs = [
  { value: "image", label: "图片", component: ImageComponent },
  { value: "video", label: "视频", component: VideoComponent },
  { value: "print", label: "打印队列", component: PrintComponent },
];
</script>

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

.control-panel {
  border-radius: 20px;
  backdrop-filter: blur(10px);
}
</style>
