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
  <router-view />
  <Login v-if="!$route.path.startsWith('/creation') && $route.path !== '/'" />
  <!-- <tab-bar /> -->
  <!-- </van-config-provider> -->
</template>

<style>
.app-wrapper {
  width: 100%;
  position: relative;
  padding: 16px;
}
/* 删除确认弹窗样式 */
.pc-modal-mask {
  position: fixed;
  z-index: 9998;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: opacity 0.3s ease;
  backdrop-filter: blur(4px);
}

.modal-container {
  width: 90%;
  max-width: 480px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  transform: scale(0.95);
  animation: scaleIn 0.3s forwards;
}

@keyframes scaleIn {
  to {
    transform: scale(1);
  }
}

.modal-header {
  padding: 24px;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  display: flex;
  align-items: center;
  gap: 15px;
}

.modal-header i {
  font-size: 1.8rem;
}

.modal-header h3 {
  font-size: 1.6rem;
  margin: 0;
}

.modal-body {
  padding: 32px 24px;
  font-size: 1.1rem;
  line-height: 1.7;
  color: #2c3e50;
  text-align: center;
}

.modal-footer {
  padding: 20px 24px;
  background-color: #f8f9fa;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  border-top: 1px solid #eee;
}

.modal-btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.modal-btn.cancel {
  background-color: #f1f2f6;
  color: #7f8c8d;
}

.modal-btn.cancel:hover {
  background-color: #dfe4ea;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(231, 76, 60, 0.3);
}

.modal-btn.confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(231, 76, 60, 0.4);
}
</style>
