<template>
  <div class="ds-modal-mask" v-if="showModal">
    <div class="password-box">
      <div class="i-carbon-locked lock-icon"></div>
      <h2>需要授权访问</h2>
      <p class="info-text">请选择活动并输入访问密码以查看受保护内容</p>

      <div class="activity-select">
        <div class="custom-select" :class="{ 'is-open': showActivityDropdown }">
          <div 
            class="select-trigger" 
            @click="toggleActivityDropdown"
            :class="{ 'has-value': selectedActivity }"
          >
            <span class="select-value">
              {{ selectedActivity ? getActivityLabel(selectedActivity) : '请选择活动' }}
            </span>
            <div class="i-carbon-chevron-down select-arrow"></div>
          </div>
          <div class="select-options" v-show="showActivityDropdown">
            <div 
              v-for="activity in activities" 
              :key="activity.value"
              class="select-option"
              :class="{ 'is-selected': selectedActivity === activity.value }"
              @click="selectActivity(activity.value)"
            >
              <div class="i-carbon-star-filled option-icon"></div>
              <span>{{ activity.label }}</span>
              <div v-if="selectedActivity === activity.value" class="i-carbon-checkmark option-check"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="password-input">
        <div class="input-wrapper">
          <input
            type="password"
            v-model="password"
            placeholder="输入访问密码"
            ref="passwordInput"
            @keyup.enter="handleLogin"
            :class="{ shake: errorMessage }"
          />
          <button class="toggle-password" @click="togglePasswordVisibility">
            <div :class="showPassword ? 'i-carbon-view-off' : 'i-carbon-view'"></div>
          </button>
        </div>
      </div>

      <button class="login-btn" @click="handleLogin">
        <div class="i-carbon-unlocked"></div> 验证并进入
      </button>

      <div
        class="message"
        :class="{ error: errorMessage, success: !errorMessage }"
      >
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getPined } from "@/api/admin";
import { STORAGE_TOKEN_KEY, STORAGE_ACTIVE_KEY } from "@/stores/mutation-type";
import { ref, onMounted } from "vue";
const emit = defineEmits(["success"]);

// 活动配置
const activities = [
  { label: '光合大会', value: 'guanghe' },
  { label: '校招', value: 'xiaozhao' }
];

// 本地存储的密码键名
// 接口请求模拟
const checkPassword = async (pwd: string, activity: string): Promise<boolean> => {
  try {
    await getPined(
      { type: "STYLED_IMAGE" },
      { headers: { Authorization: "Token " + pwd, Activity: activity } }
    );
    return true;
  } catch (error) {
    return false;
  }
};

// 响应式数据
const password = ref("");
const selectedActivity = ref("");
const showModal = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);
const showActivityDropdown = ref(false);
const passwordInput = ref(null);

// 检查本地存储的密码和活动
onMounted(async () => {
  const savedPassword = localStorage.getItem(STORAGE_TOKEN_KEY);
  const savedActivity = localStorage.getItem(STORAGE_ACTIVE_KEY);
  if (savedPassword) {
    password.value = savedPassword;
  }
  if (savedActivity) {
    selectedActivity.value = savedActivity;
  }
  
  if (password.value && selectedActivity.value) {
    const r = await checkPassword(password.value, selectedActivity.value);
    showModal.value = !r;
  } else {
    showModal.value = true;
  }

  // 点击外部关闭下拉菜单
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    if (!target.closest('.custom-select')) {
      showActivityDropdown.value = false;
    }
  });
});

// 获取活动标签
const getActivityLabel = (value: string) => {
  const activity = activities.find(a => a.value === value);
  return activity ? activity.label : '';
};

// 切换活动下拉菜单
const toggleActivityDropdown = () => {
  showActivityDropdown.value = !showActivityDropdown.value;
};

// 选择活动
const selectActivity = (value: string) => {
  selectedActivity.value = value;
  showActivityDropdown.value = false;
  errorMessage.value = ""; // 清除错误信息
};

// 切换密码可见性
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
  if (passwordInput.value) {
    passwordInput.value.type = showPassword.value ? "text" : "password";
  }
};

// 处理登录逻辑
const handleLogin = async () => {
  if (!selectedActivity.value) {
    errorMessage.value = "请选择活动";
    return;
  }
  
  if (!password.value.trim()) {
    errorMessage.value = "密码不能为空";
    return;
  }

  try {
    const isValid = await checkPassword(password.value, selectedActivity.value);

    if (isValid) {
      // 密码和活动都正确
      localStorage.setItem(STORAGE_TOKEN_KEY, password.value);
      localStorage.setItem(STORAGE_ACTIVE_KEY, selectedActivity.value);
      showModal.value = false;
      errorMessage.value = "";
      window.location.reload();
      // 这里可以执行后续业务逻辑
      emit("success");
    } else {
      errorMessage.value = "密码错误或活动选择错误，请重新输入";
      // 清空本地存储的旧密码和活动（如果有）
      localStorage.removeItem(STORAGE_TOKEN_KEY);
      localStorage.removeItem(STORAGE_ACTIVE_KEY);
    }
  } catch (error) {
    errorMessage.value = "验证失败，请重试";
    console.error("密码验证异常:", error);
  }
};
</script>

<style lang="less">
.ds-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.password-box {
  background: white;
  width: 90%;
  max-width: 360px;
  border-radius: 16px;
  padding: 25px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.4s ease-out;
  position: relative;
  overflow: hidden;
}

.password-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 50%, #1abc9c 100%);
}

@keyframes slideIn {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.password-box h2 {
  color: #2c3e50;
  margin-bottom: 8px;
  text-align: center;
  font-size: 1.5rem;
  font-weight: 600;
}

.activity-select {
  margin-bottom: 20px;
}

.custom-select {
  position: relative;
}

.select-trigger {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 0.95rem;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 44px;
}

.select-trigger:hover {
  border-color: #bdc3c7;
}

.select-trigger.has-value {
  border-color: #3498db;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f8ff 100%);
}

.custom-select.is-open .select-trigger {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.2);
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  border-bottom: none;
}

.select-value {
  color: #333;
  font-weight: 500;
}

.select-trigger:not(.has-value) .select-value {
  color: #999;
}

.select-arrow {
  color: #666;
  transition: transform 0.3s ease;
  width: 16px;
  height: 16px;
}

.custom-select.is-open .select-arrow {
  transform: rotate(180deg);
}

.select-options {
  position: absolute;
  top: calc(100% - 2px);
  left: 0;
  right: 0;
  background: white;
  border: 2px solid #3498db;
  border-top: none;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  z-index: 10;
  max-height: 160px;
  overflow-y: auto;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-5px);
    max-height: 0;
  }
  to {
    opacity: 1;
    transform: translateY(0);
    max-height: 160px;
  }
}

.select-option {
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  border-bottom: 1px solid #f0f0f0;
}

.select-option:last-child {
  border-bottom: none;
}

.select-option:hover {
  background: linear-gradient(135deg, #f8fbff 0%, #e8f4fd 100%);
}

.select-option.is-selected {
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
}

.select-option.is-selected:hover {
  background: linear-gradient(135deg, #2980b9 0%, #1f5f8b 100%);
}

.option-icon {
  color: #f39c12;
  width: 14px;
  height: 14px;
}

.select-option.is-selected .option-icon {
  color: #fff;
}

.option-check {
  margin-left: auto;
  color: #fff;
  width: 14px;
  height: 14px;
}

.password-input {
  margin-bottom: 20px;
}

.input-wrapper {
  position: relative;
}

.input-wrapper input {
  width: 100%;
  padding: 12px 45px 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 0.95rem;
  transition: all 0.3s ease;
  min-height: 44px;
}

.input-wrapper input:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.2);
  outline: none;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f8ff 100%);
}

.toggle-password {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #7f8c8d;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-password:hover {
  color: #3498db;
  background: rgba(52, 152, 219, 0.1);
}

.toggle-password div {
  width: 16px;
  height: 16px;
}

.shake {
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(52, 152, 219, 0.3);
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.login-btn:hover {
  background: linear-gradient(135deg, #2980b9 0%, #1f5f8b 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(52, 152, 219, 0.4);
}

.login-btn:active {
  transform: translateY(0px);
  box-shadow: 0 4px 15px rgba(52, 152, 219, 0.3);
}

.login-btn:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.message {
  margin-top: 15px;
  text-align: center;
  min-height: 24px;
  font-size: 0.9rem;
}

.error {
  color: #e74c3c;
}

.success {
  color: #2ecc71;
}

.info-text {
  color: #7f8c8d;
  font-size: 0.9rem;
  margin: 12px 0 20px 0;
  text-align: center;
  line-height: 1.4;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
}

.lock-icon {
  color: #3498db;
  width: 32px;
  height: 32px;
  margin: 0 auto 12px auto;
  display: block;
  filter: drop-shadow(0 2px 4px rgba(52, 152, 219, 0.3));
}

.secure-badge {
  background: #2ecc71;
  color: white;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 0.8rem;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
</style>
