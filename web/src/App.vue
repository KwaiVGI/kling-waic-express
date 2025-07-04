<script setup lang="ts">
// useHead({
//   title: "Vue3 Vant Mobile",
//   meta: [
//     {
//       name: "description",
//       content: "An mobile web apps template based on the Vue 3 ecosystem",
//     },
//     {
//       name: "theme-color",
//       content: () => (isDark.value ? "#00aba9" : "#ffffff"),
//     },
//   ],
//   link: [
//     {
//       rel: "icon",
//       type: "image/svg+xml",
//       href: () => (preferredDark.value ? "/favicon-dark.svg" : "/favicon.svg"),
//     },
//   ],
// });
</script>

<template>
  <!-- <van-config-provider :theme="mode"> -->
  <!-- <nav-bar /> -->
  <router-view></router-view>
  <!-- <tab-bar /> -->
  <!-- </van-config-provider> -->
</template>

<style scoped>
.app-wrapper {
  width: 100%;
  position: relative;
  padding: 16px;
}
</style>
