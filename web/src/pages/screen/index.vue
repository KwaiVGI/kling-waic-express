<script lang="ts" setup>
import LogoutButton from '@/components/LogoutButton.vue'
</script>

<template>
  <div
    class="creation-index bg-gray-50 flex flex-col min-h-100vh items-center justify-center"
  >
    <LogoutButton :transparent="true" />
    <h1 class="text-24px text-blue font-bold mb-8">
      选择创作类型
    </h1>

    <div class="creation-types gap-20px grid grid-cols-2 max-w-400px w-80%">
      <router-link
        to="/screen/image"
        class="creation-card p-25px text-center rounded-16px bg-white shadow-md transition-all active:scale-98"
      >
        <van-icon name="photograph" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">
          图片创作
        </h2>
      </router-link>

      <router-link
        to="/screen/video"
        class="creation-card p-25px text-center rounded-16px bg-white shadow-md transition-all active:scale-98"
      >
        <van-icon name="video-o" size="60" color="#1989fa" />
        <h2 class="text-18px font-bold mt-15px">
          视频创作
        </h2>
      </router-link>
    </div>
  </div>
</template>

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
