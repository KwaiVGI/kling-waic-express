<template>
  <div class="ds-modal-mask" v-if="showModal">
    <div class="password-box">
      <i class="fas fa-lock lock-icon"></i>
      <h2>需要授权访问</h2>
      <p class="info-text">请输入访问密码以查看受保护内容</p>

      <div class="password-input">
        <input
          type="password"
          v-model="password"
          placeholder="输入访问密码"
          ref="passwordInput"
          @keyup.enter="handleLogin"
          :class="{ shake: errorMessage }"
        />
        <button class="toggle-password" @click="togglePasswordVisibility">
          {{ showPassword ? "Hide" : "Show" }}
        </button>
      </div>

      <button class="login-btn" @click="handleLogin">
        <i class="fas fa-unlock-alt"></i> 验证并进入
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
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";
import { ref, onMounted } from "vue";
const emit = defineEmits(["success"]);
// 本地存储的密码键名
// 接口请求模拟
const checkPassword = async (pwd: string): Promise<boolean> => {
  try {
    await getPined(
      { type: "STYLED_IMAGE" },
      { headers: { Authorization: "Token " + pwd } }
    );
    return true;
  } catch (error) {
    return false;
  }
};

// 响应式数据
const password = ref("");
const showModal = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);
const passwordInput = ref(null);

// 检查本地存储的密码
onMounted(async () => {
  const savedPassword = localStorage.getItem(STORAGE_TOKEN_KEY);
  if (savedPassword) {
    password.value = savedPassword;
  }
  const r = await checkPassword(password.value);
  showModal.value = !r;
});

// 切换密码可见性
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
  if (passwordInput.value) {
    passwordInput.value.type = showPassword.value ? "text" : "password";
  }
};

// 处理登录逻辑
const handleLogin = async () => {
  if (!password.value.trim()) {
    errorMessage.value = "密码不能为空";
    return;
  }

  try {
    const isValid = await checkPassword(password.value);

    if (isValid) {
      // 密码正确
      localStorage.setItem(STORAGE_TOKEN_KEY, password.value);
      showModal.value = false;
      errorMessage.value = "";
      window.location.reload();
      // 这里可以执行后续业务逻辑
      emit("success");
    } else {
      errorMessage.value = "密码错误，请重新输入";
      // 清空本地存储的旧密码（如果有）
      localStorage.removeItem(STORAGE_TOKEN_KEY);
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
  max-width: 400px;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.4s ease-out;
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
  margin-bottom: 20px;
  text-align: center;
}

.password-input {
  position: relative;
  margin-bottom: 25px;
}

.password-input input {
  width: 100%;
  padding: 15px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.password-input input:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.2);
  outline: none;
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
  font-size: 1.2rem;
}

.login-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(to right, #3498db, #2980b9);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(52, 152, 219, 0.3);
}

.login-btn:hover {
  background: linear-gradient(to right, #2980b9, #2573a7);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 152, 219, 0.4);
}

.login-btn:active {
  transform: translateY(1px);
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
  margin-top: 15px;
  text-align: center;
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
  font-size: 2rem;
  margin-bottom: 20px;
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
