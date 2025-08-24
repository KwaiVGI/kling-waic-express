<template>
  <div class="qr-container">
    <LogoutButton :transparent="true" />
    <div ref="qrCodeElement" class="qr-code"></div>
    <div class="text-section">
      <h2 class="chinese-title">
        {{
          $route.params.type === "image"
            ? "可灵 AI - 图片 - 风格转绘"
            : "可灵 AI - 视频 - 特效盲盒"
        }}
      </h2>
      <p class="english-subtitle">
        {{
          $route.params.type === "image"
            ? "KlingAI - Image - Restyle Collage"
            : "KlingAI - Video - Effect Blind Box"
        }}
      </p>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute } from "vue-router";
import LogoutButton from "@/components/LogoutButton.vue";
import QRCodeStyling from "qr-code-styling";
import { getLatestToken } from "@/api";
import { type TaskType } from "@/api/type";
const route = useRoute();
const qrCodeElement = ref<HTMLElement | null>(null);
const qrCode = ref<QRCodeStyling | null>(null);
const token = ref<string>("");
let timer: number | null = null;
// token 10分钟失效，前端5分钟刷一次
const tokenPollingInterval = 1000 * 5;
// 将路由参数映射到TaskType
const getTaskType = (): TaskType => {
  const routeType = route.params.type as string;
  return routeType === "image" ? "STYLED_IMAGE" : "VIDEO_EFFECT";
};

// 获取token的API请求
const fetchToken = async (): Promise<string> => {
  try {
    const taskType = getTaskType();
    const data = await getLatestToken(taskType);
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
  if (route.query.token) {
    // Token handling removed
  }
  token.value = await fetchToken();
  initQrCode();
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
  padding: 20px;
}

.qr-code {
  border: 1px solid white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 32px;
}

.text-section {
  text-align: center;
  max-width: 400px;
}

.chinese-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px 0;
  line-height: 1.4;
  letter-spacing: 0.5px;
}

.english-subtitle {
  font-size: 16px;
  font-weight: 400;
  color: #666666;
  margin: 0;
  line-height: 1.5;
  letter-spacing: 0.5px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    "Helvetica Neue", Arial, sans-serif;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .qr-container {
    padding: 16px;
  }

  .chinese-title {
    font-size: 20px;
  }

  .english-subtitle {
    font-size: 14px;
  }
}
</style>
