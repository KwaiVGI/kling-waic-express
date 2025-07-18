<template>
  <div class="qr-container">
    <div ref="qrCodeElement" class="qr-code"></div>
    <span class="mt-16px">{{
      $route.params.type === "image"
        ? "可灵 AI 图片-风格转绘"
        : "可灵 AI 视频-特效盲盒"
    }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute } from "vue-router";
import QRCodeStyling from "qr-code-styling";
import { getLatestToken } from "@/api";
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";

const route = useRoute();
const qrCodeElement = ref<HTMLElement | null>(null);
const qrCode = ref<QRCodeStyling | null>(null);
const token = ref<string>("");
let timer: number | null = null;
// token 10分钟失效，前端5分钟刷一次
const tokenPollingInterval = 1000 * 5;

// 获取token的API请求
const fetchToken = async (): Promise<string> => {
  try {
    const data = await getLatestToken();
    return data.value;
  } catch (error) {
    console.warn("获取token失败:", error);
    return "mock-token";
  }
};

// 初始化二维码生成器 https://qr-code-styling.com/
const initQrCode = () => {
  qrCode.value = new QRCodeStyling({
    width: 300,
    height: 300,
    data: "",
    image: "https://p2-kling.klingai.com/kcdn/cdn-kcdn112452/logo-80x80.png",
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
  }, tokenPollingInterval) as unknown as number;
};

// 初始化
onMounted(async () => {
  initQrCode();
  localStorage.setItem(STORAGE_TOKEN_KEY, route.query.token as string);
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
