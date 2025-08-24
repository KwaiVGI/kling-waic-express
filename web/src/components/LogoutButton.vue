<template>
  <div
    class="logout-container"
    :class="{ 'transparent-mode': transparent }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="logout-dropdown" :class="{ show: showDropdown }">
      <button
        class="logout-trigger"
        @click="toggleDropdown"
        :class="{ visible: !transparent || isHovered }"
      >
        {{ currentActivityLabel }}
      </button>

      <div
        class="dropdown-menu"
        v-show="showDropdown"
        @mouseenter="handleDropdownMouseEnter"
        @mouseleave="handleDropdownMouseLeave"
      >
        <div class="current-activity">
          <span class="activity-label">当前活动:</span>
          <span class="activity-name">{{ currentActivityLabel }}</span>
        </div>
        <button class="dropdown-item" @click="switchActivity">
          <div class="i-carbon-arrows-horizontal"></div>
          切换活动
        </button>
        <button class="dropdown-item logout" @click="logout">
          <div class="i-carbon-logout"></div>
          退出登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { STORAGE_TOKEN_KEY, STORAGE_ACTIVE_KEY } from "@/stores/mutation-type";

interface Props {
  transparent?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  transparent: false,
});

const showDropdown = ref(false);
const isHovered = ref(false);
const currentActivity = ref("");
let hideTimeout: number | null = null;

// 活动配置
const activities = [
  { label: "光合大会", value: "guanghe" },
  { label: "校招", value: "xiaozhao" },
];

const currentActivityLabel = computed(() => {
  const activity = activities.find((a) => a.value === currentActivity.value);
  return activity ? activity.label : "未知活动";
});

const handleMouseEnter = () => {
  if (props.transparent) {
    isHovered.value = true;
    // 清除隐藏定时器
    if (hideTimeout) {
      clearTimeout(hideTimeout);
      hideTimeout = null;
    }
  }
};

const handleMouseLeave = () => {
  if (props.transparent) {
    // 延迟隐藏，给用户时间移动到下拉菜单
    hideTimeout = setTimeout(() => {
      isHovered.value = false;
      if (!showDropdown.value) {
        // 只有在下拉菜单没有显示时才隐藏按钮
        isHovered.value = false;
      }
    }, 200) as unknown as number;
  }
};

const handleDropdownMouseEnter = () => {
  // 鼠标进入下拉菜单时清除隐藏定时器
  if (hideTimeout) {
    clearTimeout(hideTimeout);
    hideTimeout = null;
  }
};

const handleDropdownMouseLeave = () => {
  if (props.transparent) {
    // 鼠标离开下拉菜单时延迟隐藏
    hideTimeout = setTimeout(() => {
      isHovered.value = false;
      showDropdown.value = false;
    }, 200) as unknown as number;
  }
};

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value;
};

const switchActivity = () => {
  localStorage.removeItem(STORAGE_TOKEN_KEY);
  localStorage.removeItem(STORAGE_ACTIVE_KEY);
  window.location.reload();
};

const logout = () => {
  localStorage.removeItem(STORAGE_TOKEN_KEY);
  localStorage.removeItem(STORAGE_ACTIVE_KEY);
  window.location.reload();
};

onMounted(() => {
  currentActivity.value = localStorage.getItem(STORAGE_ACTIVE_KEY) || "";

  // 点击外部关闭下拉菜单
  document.addEventListener("click", (e) => {
    const target = e.target as HTMLElement;
    if (!target.closest(".logout-dropdown")) {
      showDropdown.value = false;
      if (props.transparent) {
        isHovered.value = false;
      }
    }
  });
});

onUnmounted(() => {
  // 清理定时器
  if (hideTimeout) {
    clearTimeout(hideTimeout);
  }
});
</script>

<style scoped>
.logout-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.logout-dropdown {
  position: relative;
}

.logout-trigger {
  min-width: 80px;
  height: 36px;
  padding: 0 12px;
  border-radius: 18px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.transparent-mode .logout-trigger {
  background: transparent;
  color: transparent;
  box-shadow: none;
}

.transparent-mode .logout-trigger.visible {
  background: rgba(255, 255, 255, 0.9);
  color: #666;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.logout-trigger:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}

.dropdown-menu {
  position: absolute;
  top: 45px;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.current-activity {
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  font-size: 12px;
}

.activity-label {
  color: #666;
}

.activity-name {
  color: #333;
  font-weight: 600;
  margin-left: 4px;
}

.dropdown-item {
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
  transition: background-color 0.2s ease;
}

.dropdown-item div {
  width: 16px;
  height: 16px;
}

.dropdown-item:hover {
  background: #f8f9fa;
}

.dropdown-item.logout {
  color: #dc3545;
  border-top: 1px solid #e9ecef;
}

.dropdown-item.logout:hover {
  background: #fff5f5;
}
</style>
