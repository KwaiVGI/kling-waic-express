<template>
  <div
    class="creation-index flex flex-col items-center justify-center min-h-100vh bg-gray-50"
  >
    <h1 class="text-24px font-bold text-blue mb-8">选择创作类型</h1>

    <div class="creation-types grid grid-cols-2 gap-20px w-80% max-w-400px">
      <router-link
        to="/creation/image"
        class="creation-card bg-white rounded-16px shadow-md p-25px text-center transition-all active:scale-98"
      >
        <van-icon name="photograph" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">图片创作</h2>
        <p class="text-14px text-gray-600 mt-2">将照片转化为艺术作品</p>
      </router-link>

      <router-link
        to="/creation/video"
        class="creation-card bg-white rounded-16px shadow-md p-25px text-center transition-all active:scale-98"
      >
        <van-icon name="video-o" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">视频创作</h2>
        <p class="text-14px text-gray-600 mt-2">从照片生成创意视频</p>
      </router-link>
    </div>

    <p
      class="warning-tip mt-30px text-12px text-orange flex items-center justify-center gap-4px"
    >
      <van-icon name="warning-o" color="#ff976a" />
      内容由AI生成，禁止利用功能从事违法活动。
    </p>
  </div>
</template>

<script lang="ts" setup>
import { showToast } from "vant";
</script>

<style scoped>
.creation-index {
  background: linear-gradient(135deg, #f8f9ff 0%, #e8f4ff 100%);
}

.creation-card {
  &:active {
    transform: scale(0.98);
  }
}
</style>
