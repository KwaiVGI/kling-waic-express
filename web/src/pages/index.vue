<template>
  <div class="container">
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
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

interface NavItem {
  title: string;
  path: string;
}

const navItems = ref<NavItem[]>([
  { title: "二维码", path: "/qr" },
  { title: "生图/生视频", path: "/creation" },
  { title: "实时大屏", path: "/screen" },
  { title: "大屏控制台", path: "/admin" },
]);

const navigateTo = (path: string) => {
  router.push(path);
};
</script>

<style scoped>
.container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 24px;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.card-content {
  text-align: center;
  padding: 20px;
}

.card-icon {
  margin-bottom: 16px;
}

.icon-placeholder {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  border-radius: 50%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
</style>
