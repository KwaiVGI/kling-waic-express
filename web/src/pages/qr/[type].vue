<template>
  <div class="qr-container">
    <div ref="qrCodeElement" class="qr-code"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute } from "vue-router";
import QRCodeStyling from "qr-code-styling";

const route = useRoute();
const qrCodeElement = ref<HTMLElement | null>(null);
const qrCode = ref<QRCodeStyling | null>(null);
const token = ref<string>("");
let timer: number | null = null;

// 获取token的API请求
const fetchToken = async (): Promise<string> => {
  //   try {
  //     // 替换为你的实际API端点
  //     const response = await fetch("https://api.example.com/get-token");
  //     const data = await response.json();
  //     return data.token;
  //   } catch (error) {
  //     console.error("获取token失败:", error);
  //     return "";
  //   }
  return Math.random().toString(36).substring(2, 15);
};

// 初始化二维码生成器 https://qr-code-styling.com/
const initQrCode = () => {
  qrCode.value = new QRCodeStyling({
    width: 300,
    height: 300,
    data: "",
    image:
      "https://p9-passport.byteacctimg.com/img/user-avatar/4fb1f1f5e20cf37f0c49957885d58b38~100x100.awebp",
    margin: 10,
    qrOptions: {
      typeNumber: 0,
      mode: "Byte",
      errorCorrectionLevel: "Q",
    },
    imageOptions: {
      hideBackgroundDots: true,
      imageSize: 0.4,
      margin: 5,
    },
    dotsOptions: {
      color: "#000",
      type: "rounded",
    },
    backgroundOptions: {
      color: "#fff",
    },
    cornersSquareOptions: {
      color: "#000",
      type: "extra-rounded",
    },
  });
};
// 更新二维码内容
const updateQrCode = () => {
  if (!qrCode.value || !token.value) return;

  const url = `${window.location.origin}/creation/${route.params.type}?token=${token.value}`;
  qrCode.value.update({
    data: url,
  });

  if (qrCodeElement.value) {
    qrCode.value.append(qrCodeElement.value);
  }
};

// 定时更新token
const startTokenPolling = () => {
  timer = setInterval(async () => {
    const newToken = await fetchToken();
    if (newToken && newToken !== token.value) {
      token.value = newToken;
    }
  }, 3000) as unknown as number;
};

// 初始化
onMounted(async () => {
  initQrCode();
  token.value = await fetchToken();
  startTokenPolling();
});

// 监听token变化
watch(token, (newVal) => {
  if (newVal) {
    updateQrCode();
  }
});

// 清理定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped>
.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.qr-code {
  border: 1px solid white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}
</style>
