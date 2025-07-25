<template>
  <div class="ds-container">
    <!-- <div
      v-for="(item, index) in navItems"
      :key="index"
      class="card"
      @click="navigateTo(item.path)"
    >
      <div class="card-content">
        <div class="card-icon">
          <div class="icon-placeholder"></div>
        </div>
        <h3 class="card-title">{{ item.title }}</h3>
      </div>
    </div> -->
    <h2 class="text-center">
      欢迎来展台现场零距离体验生成式AI魅力，7月26日-29日，锁定上海世博展览馆H1-B821！<br />期待与您相遇！
    </h2>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.ds-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 10vw;
  box-sizing: border-box;
}
</style>
