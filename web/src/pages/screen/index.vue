<template>
  <div
    class="creation-index flex flex-col items-center justify-center min-h-100vh bg-gray-50"
  >
    <h1 class="text-24px font-bold text-blue mb-8">选择创作类型</h1>

    <div class="creation-types grid grid-cols-2 gap-20px w-80% max-w-400px">
      <router-link
        to="/screen/image"
        class="creation-card bg-white rounded-16px shadow-md p-25px text-center transition-all active:scale-98"
      >
        <van-icon name="photograph" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">图片创作</h2>
      </router-link>

      <router-link
        to="/screen/video"
        class="creation-card bg-white rounded-16px shadow-md p-25px text-center transition-all active:scale-98"
      >
        <van-icon name="video-o" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">视频创作</h2>
      </router-link>
    </div>
  </div>
</template>

<script lang="ts" setup></script>

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
