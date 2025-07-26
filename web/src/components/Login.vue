<template>
  <div v-if="showModal" class="ds-modal-mask">
    <div class="modal-wrapper">
      <div class="modal-container">
        <h3>请输入访问密码</h3>
        <div class="input-group">
          <input
            v-model="password"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>
        </div>
        <div class="button-group">
          <button @click="handleLogin">登录</button>
        </div>
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

// 检查本地存储的密码
onMounted(async () => {
  const savedPassword = localStorage.getItem(STORAGE_TOKEN_KEY);
  if (savedPassword) {
    password.value = savedPassword;
  }
  const r = await checkPassword(password.value);
  showModal.value = !r;
});

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
@primary-color: #409eff;
@error-color: #f56c6c;
@modal-bg: rgba(0, 0, 0, 0.5);

.ds-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: @modal-bg;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.modal-container {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  h3 {
    margin-top: 0;
    margin-bottom: 20px;
    text-align: center;
    color: #333;
  }
}

.input-group {
  margin-bottom: 20px;

  input {
    width: 100%;
    height: 40px;
    padding: 0 15px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    box-sizing: border-box;

    &:focus {
      border-color: @primary-color;
      outline: none;
    }
  }
}

.error-message {
  color: @error-color;
  font-size: 12px;
  margin-top: 8px;
  height: 20px;
}

.button-group {
  text-align: center;

  button {
    background-color: @primary-color;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;

    &:hover {
      background-color: darken(@primary-color, 10%);
    }

    &:active {
      background-color: darken(@primary-color, 15%);
    }
  }
}
</style>
