<template>
  <div class="tab-container">
    <!-- Tab 按钮 -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="activeTab = tab.value"
        :class="{ active: activeTab === tab.value }"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab 内容（使用动态组件或插槽） -->
    <div class="tab-content">
      <!-- 方案1：动态组件（适合复杂内容） -->
      <component :is="activeComponent" v-if="activeComponent" />

      <!-- 方案2：插槽（适合简单内容） -->
      <slot :name="activeTab"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";

// 定义单个 Tab 的类型
interface TabItem {
  value: string; // 唯一标识（如 'image', 'video'）
  label: string; // 显示名称（如 '图片', '视频'）
  component?: any; // 动态组件（可选）
}

// Props 定义
const props = defineProps<{
  tabs: TabItem[]; // Tab 列表
  defaultActive?: string; // 默认选中的 Tab
}>();

// 当前选中的 Tab
const activeTab = ref(props.defaultActive || props.tabs[0]?.value);

// 计算当前激活的组件（方案1）
const activeComponent = computed(() => {
  const tab = props.tabs.find((t) => t.value === activeTab.value);
  return tab?.component || null;
});
</script>

<style scoped>
.tab-container {
  width: 100%;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.tabs button {
  padding: 4px 16px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #ddd;
  background: #f5f5f5;
  border-radius: 4px;
}

.tabs button.active {
  background: #4361ee;
  color: white;
  border-color: #4361ee;
}

.tab-content {
  /* padding: 16px;
  border: 1px solid #eee;
  border-radius: 4px; */
}
</style>
