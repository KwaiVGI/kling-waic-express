<script lang="ts" setup>
import { showToast } from "vant";
import useCreation from "@/composables/useCreation";
import type { CreationType } from "@/composables/useCreation";
import { getTaskStatus, newTask, PrintingStatus } from "@/api/creation";
import { fetchConfig } from "@/api/admin";
import type { AdminConfig } from "@/api/admin";
import {
  STORAGE_ACTIVE_KEY,
  STORAGE_USER_TOKEN_KEY,
} from "@/stores/mutation-type";
import { useZoom } from "@/composables/useZoom";
import { updateQueryParams } from "@/utils/url";
import { waitWithAbort } from "@/utils/time";
import { locale } from "@/utils/i18n";
import type { Locale } from "@/utils/i18n";
import {
  bannerImageEn,
  bannerImageZh,
  bannerVideoEn,
  bannerVideoZh,
  imageTipEn,
  imageTipZh,
  logoEn,
  logoZh,
} from "./const";

const route = useRoute();
const { t } = useI18n();
// 从路由参数获取类型
const type = ref<string>((route.params.type as CreationType) || "image");

// 管理员配置
const adminConfig = ref<AdminConfig | null>(null);

// 活动主题配置
const isXiaozhaoActivity = computed(() => route.query.activity === "xiaozhao");

const assets = computed(() => {
  const isZh = locale.value === "zh-CN";

  if (isXiaozhaoActivity.value) {
    // 校招活动主题配置
    const bannerImage =
      "https://ali.a.yximgs.com/kos/nlav12119/LYXfjgtf_2025-08-25-22-36-33.png";
    const bannerVideo = isZh ? bannerVideoZh : bannerVideoEn;
    return {
      logo: logoZh, // 校招活动只使用中文logo
      xiaozhaoLogo:
        "https://tx.a.yximgs.com/kos/nlav12119/ffBJkwTW_2025-08-25-22-34-08.png",
      banner: type.value === "image" ? bannerImage : bannerVideo,
      imageTip: isZh ? imageTipZh : imageTipEn,
    };
  } else {
    // 默认主题配置
    const bannerImage = isZh ? bannerImageZh : bannerImageEn;
    const bannerVideo = isZh ? bannerVideoZh : bannerVideoEn;
    return {
      logo: isZh ? logoZh : logoEn,
      banner: type.value === "image" ? bannerImage : bannerVideo,
      imageTip: isZh ? imageTipZh : imageTipEn,
    };
  }
});

const screenTip = computed(() => {
  return type.value === "image"
    ? t("descriptions.imageScreenTip")
    : t("descriptions.videoScreenTip");
});

// 打印状态文本
const printStatusText = computed(() => {
  if (!printStatus.value) return null;

  // 检查状态是否在有效的 PrintingStatus 枚举中
  const validStatuses = Object.values(PrintingStatus);
  if (!validStatuses.includes(printStatus.value as PrintingStatus)) {
    return null;
  }

  switch (printStatus.value) {
    case READY:
    case QUEUING:
      return t("print.status.waiting", { count: printAheadCount.value || 0 });
    case PRINTING:
      return t("print.status.printing");
    case COMPLETED:
      return t("print.status.completed");
    case FAILED:
    case CANCELLED:
      return t("print.status.failed");
    default:
      return null;
  }
});

// 暴露 PrintingStatus 给模板使用
const { READY, QUEUING, PRINTING, COMPLETED, FAILED, CANCELLED } =
  PrintingStatus;

// 使用组合函数
const {
  uploaderRef,
  fileList,
  uploadedImage,
  generatedResult,
  isGenerating,
  isSaving,
  isPrinting,
  printStatus,
  printAheadCount,
  handleUpload,
  uploading,
  openPreview,
  handleDelete,
  handleReplace,
  generate,
  save,
  backToEdit,
  printImage,
  stopPrintStatusPolling,
} = useCreation(type.value as "image" | "video");

const currentImageNo = ref("");
const sourceImageUrl = ref("");

// 生成
async function doGenerate(
  url: string,
  type: CreationType,
  signal?: AbortSignal
): Promise<string> {
  // 1. 创建新任务
  const { name } = await newTask({
    url,
    type: type === "image" ? "STYLED_IMAGE" : "VIDEO_EFFECT",
  });

  // 2. 轮询任务状态
  let status = "";
  const maxAttempts = 1800; // 最大尝试次数，防止无限循环
  const delay = 2000; // 每次轮询间隔2秒

  // await wait(10 * 1000);
  await waitWithAbort(10 * 1000, signal);
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    if (signal?.aborted) throw new DOMException("Aborted", "AbortError");
    // 获取任务状态
    const result = await getTaskStatus({
      name,
      type: type === "image" ? "STYLED_IMAGE" : "VIDEO_EFFECT",
      locale: locale.value === "en-US" ? "US" : "CN",
    });

    status = result.status;

    if (status === "SUCCEED") {
      const { url } = result.outputs || {};
      const { image } = result.input || {};
      if (url) {
        currentImageNo.value = result.name;
        sourceImageUrl.value = image;
        return url; // 成功返回URL
      }
      throw new Error();
    }

    if (status === "FAILED") {
      throw new Error();
    }

    // 如果未完成，等待一段时间再继续
    await waitWithAbort(delay, signal);
  }

  throw new Error();
}

let controller: AbortController | null = null;

function handleCancel() {
  if (controller) {
    controller.abort();
  }
}

// 处理生成
async function handleGenerate() {
  if (isGenerating.value) {
    return;
  }
  if (!uploadedImage.value) {
    showToast({
      // icon: "https://tx.a.yximgs.com/kos/nlav12119/bIzvPqKP_2025-07-21-19-58-29.png",
      message: t("upload.placeholder"),
      position: "bottom",
    });
    return;
  }
  try {
    controller = new AbortController();
    await generate(doGenerate, controller.signal);
  } catch (error) {
    const errorMap = {
      1203: t("errors.api.serviceUnavailable"),
      1300: t("errors.api.notEffective"),
      1301: t("errors.api.invalidRequest"),
      1302: t("errors.api.rateLimit"),
      1303: t("errors.api.quotaExceeded"),
      1304: t("errors.api.networkIssue"),
      5000: t("errors.api.systemError"),
      5001: t("errors.api.maintenance"),
      5002: t("errors.api.busy"),
    };
    if (error.name === "AbortError") {
      return;
    } else if (error === 401) {
      showToast(t("errors.generic.authFailed"));
    } else if (error.message === "NO_HUMAN_DETECTED") {
      showToast(t("upload.noFaceDetected"));
    } else if (errorMap[error.message]) {
      showToast(errorMap[error.message]);
    } else if (error.message === "TOO_MANY_REQUESTS") {
      showToast(t("errors.api.quotaExceeded"));
    } else {
      showToast(t("upload.generationFailed"));
    }
    return;
  }

  // 将图片URL放到查询参数上
  updateQueryParams({
    sourceUrl: sourceImageUrl.value,
    resultUrl: generatedResult.value,
    lang: locale.value,
  });
}

// 处理保存
function handleSave() {
  if (type.value === "image") {
    save("", "jpg");
  } else {
    save("", "mp4");
  }
}

function onLocaleChange(l: Locale) {
  updateQueryParams(
    {
      lang: l,
    },
    "replace"
  );
}

const step1Ref = ref<HTMLElement | null>(null);
const step2Ref = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const step1Zoom = useZoom(step1Ref);
const step2Zoom = useZoom(step2Ref);

// 获取管理员配置
async function loadAdminConfig() {
  try {
    adminConfig.value = await fetchConfig();
  } catch (error) {
    console.error("获取管理员配置失败:", error);
    // 如果获取失败，使用默认配置
    adminConfig.value = {
      allowPrint: true,
      imageServiceOnline: true,
      videoServiceOnline: true,
      imageTokenExpireInSeconds: 180,
      videoTokenExpireInSeconds: 600,
      maxPrinterJobCount: 10,
      screenImageRatios: { first: 9, second: 16 },
      screenVideoRatios: { first: 9, second: 16 },
    };
  }
}
onMounted(() => {
  if (route.query.token) {
    localStorage.setItem(STORAGE_USER_TOKEN_KEY, route.query.token as string);
  }
  if (route.query.activity) {
    localStorage.setItem(STORAGE_ACTIVE_KEY, route.query.activity as string);
  } else {
    localStorage.setItem(STORAGE_ACTIVE_KEY, "xiaozhao");
  }
  console.log({ userAgent: navigator.userAgent });
  if (route.query.resultUrl) {
    generatedResult.value = decodeURIComponent(route.query.resultUrl as string);
  }
  if (route.query.sourceUrl) {
    uploadedImage.value = decodeURIComponent(route.query.sourceUrl as string);
    sourceImageUrl.value = decodeURIComponent(route.query.sourceUrl as string);
  }
  // 加载管理员配置
  loadAdminConfig();
});

onUnmounted(() => {
  // 清理打印状态轮询
  stopPrintStatusPolling();
});
</script>

<template>
  <div
    id="h5App"
    ref="containerRef"
    class="creation-container flex flex-col items-center relative !min-h-100vh"
    :class="{ 'xiaozhao-theme': isXiaozhaoActivity }"
  >
    <div
      ref="step1Ref"
      :style="{ zoom: step1Zoom }"
      class="step-1 pb-28px flex flex-col w-full items-center"
    >
      <div
        v-show="!generatedResult"
        class="mb-28px mt-28px flex flex-col w-full items-center relative"
      >
        <div
          class="mb-22px ml-20px flex gap-12px h-32px items-center self-start"
        >
          <!-- 校招活动显示额外的logo -->
          <img
            v-if="isXiaozhaoActivity && assets.xiaozhaoLogo"
            class="h-32px"
            :src="assets.xiaozhaoLogo"
            alt="校招logo"
          />
          <img class="h-32px w-120px" :src="assets.logo" alt="" />
        </div>
        <img class="h-130px w-414px" :src="assets.banner" alt="" />
        <!-- 校招活动不显示语言切换按钮 -->
        <LangSwitcher
          v-if="!isXiaozhaoActivity"
          class="right-20px top-0 absolute"
          @change="onLocaleChange"
        />
      </div>
      <!-- 上传区域 -->
      <div
        v-show="(!uploadedImage && !generatedResult) || uploading"
        class="upload-bg-white p-20px text-center rounded-24px h-360px w-360px box-border relative"
      >
        <van-uploader
          ref="uploaderRef"
          v-model="fileList"
          :after-read="handleUpload"
          :max-count="1"
          reupload
          :preview-image="false"
          accept="image/*"
        >
          <div
            class="upload-area rounded-12px flex flex-col w-320px items-center justify-center"
            :class="type === 'image' ? 'h-254px' : 'h-320px'"
          >
            <div
              v-if="uploading"
              class="uploading flex flex-col gap-12px h-full w-full items-center justify-center"
            >
              <van-loading type="circular" color="#0B8A1B" />
              <div class="upload-text-white text-14px">
                {{ $t("upload.uploading") }}
              </div>
            </div>
            <template v-else>
              <IconSvg name="add-image" :size="32" />
              <p class="upload-text-white text-14px mt-10px">
                {{ $t("actions.uploadPhoto") }}
              </p>
            </template>
          </div>
        </van-uploader>
        <div
          v-if="type === 'image' && adminConfig?.allowPrint !== false"
          class="h-124px w-360px bottom-0 left-0 absolute"
        >
          <img class="h-full w-full" :src="assets.imageTip" alt="" />
        </div>
      </div>

      <!-- 图片预览区域 -->
      <div
        v-if="uploadedImage && !generatedResult && !uploading"
        class="upload-bg-white p-20px text-center rounded-24px h-360px w-360px box-border relative"
      >
        <div
          class="rounded-12px flex h-254px w-320px items-center justify-center relative overflow-hidden"
        >
          <div
            class="select blur-bg h-full w-full left-0 top-0 absolute bg-cover bg-center blur-20px"
            :style="{ backgroundImage: `url(${uploadedImage})` }"
          />
          <img
            :src="uploadedImage"
            alt="上传的图片"
            class="h-full w-full relative z-10 object-contain object-center"
            @click="openPreview(uploadedImage)"
          />
        </div>
        <div class="mt-16px flex gap-8px h-48px w-full justify-between">
          <button
            :disabled="isGenerating"
            class="btn-bg text-black leading-1 rounded-8px flex flex-1 gap-6px h-full items-center justify-center disabled:text-#B0B4B8ff"
            @click="handleReplace()"
          >
            <IconSvg
              name="replace"
              :color="isGenerating ? '#B0B4B8' : 'black'"
            />
            <span> {{ $t("actions.replace") }} </span>
          </button>
          <button
            :disabled="isGenerating"
            class="btn-bg color-black leading-1 rounded-8px flex flex-1 gap-6px h-full items-center justify-center disabled:text-#B0B4B8ff"
            @click="handleDelete"
          >
            <IconSvg
              name="delete"
              :color="isGenerating ? '#B0B4B8' : 'black'"
            />
            <span> {{ $t("actions.delete") }} </span>
          </button>
        </div>
      </div>

      <!-- 生成按钮区域 -->
      <div
        v-if="!generatedResult"
        class="generate-section mt-48px text-center w-full"
      >
        <van-button
          round
          type="primary"
          :loading="isGenerating"
          :loading-text="$t('status.processing')"
          class="generate-btn font-bold !text-20px !text-black !h-56px !w-320px"
          @click="handleGenerate"
        >
          {{ $t("actions.generateNow") }}
        </van-button>
        <div
          class="warning-tip text-12px text-#5E6266ff mt-30px flex flex-col gap-8px items-center justify-center"
        >
          <span>{{ $t("descriptions.aiDisclaimer") }}</span>
          <span>{{ screenTip }}</span>
        </div>
      </div>
    </div>
    <!-- 生成的结果区域 -->
    <div
      v-show="generatedResult"
      class="result-section-bg bottom-0 left-0 right-0 top-0 absolute"
    />
    <div
      v-if="generatedResult"
      ref="step2Ref"
      :style="{ zoom: step2Zoom }"
      class="result-section animate-slideUp px-18px py-40px flex flex-col w-full items-center box-border relative z-10"
    >
      <div
        v-if="type === 'image' && adminConfig?.allowPrint !== false"
        class="rounded-8px h-570px w-380px relative"
      >
        <img
          :src="generatedResult"
          alt="生成的图片"
          class="rounded-8px h-full w-full shadow-sm object-cover object-center"
        />
      </div>
      <div v-else class="rounded-8px h-604px w-340px relative">
        <VideoPlayer
          :src="generatedResult"
          :poster="uploadedImage"
          autoplay
          class="rounded-8px h-full w-full shadow-sm overflow-hidden object-cover object-center"
        />
      </div>

      <!-- 打印状态显示 -->
      <div
        v-if="type === 'image' && printStatusText"
        class="print-status-display text-14px font-medium mb-8px mt-16px px-16px py-8px text-center rounded-8px"
        :class="[
          printStatus === COMPLETED
            ? 'bg-green-100 text-green-700'
            : printStatus === FAILED || printStatus === CANCELLED
            ? 'bg-red-100 text-red-700'
            : printStatus === PRINTING
            ? 'bg-blue-100 text-blue-700'
            : 'bg-yellow-100 text-yellow-700',
        ]"
        :style="{ width: type === 'image' ? '100%' : '340px' }"
      >
        <div class="flex gap-8px items-center justify-center">
          <span>{{ printStatusText }}</span>
        </div>
      </div>

      <div
        class="result-actions mt-24px flex gap-8px h-48px"
        :class="type === 'image' ? 'w-full' : 'w-340px'"
      >
        <!-- <van-button
          type="default"
          @click="backToEdit"
          class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          {{ $t("actions.back") }}
        </van-button> -->
        <van-button
          v-if="type === 'image' && adminConfig?.allowPrint !== false"
          icon="print"
          type="default"
          :loading="isPrinting"
          class="action-btn text-16px font-500 flex-1 h-full shadow-sm !text-14px !rounded-8px !border-none"
          @click="printImage(currentImageNo)"
        >
          {{ $t("actions.print") }}
        </van-button>

        <van-button
          type="default"
          :loading="isSaving"
          class="action-btn text-16px font-500 flex-1 h-full shadow-sm !text-14px !rounded-8px !border-none"
          @click="handleSave"
        >
          {{ $t("actions.save") }}
        </van-button>
      </div>
      <div
        class="warning-tip text-12px text-#5E6266ff mt-30px flex flex-col gap-8px items-center justify-center"
      >
        <span>{{ $t("descriptions.aiDisclaimer") }}</span>
        <span>{{ screenTip }}</span>
      </div>
    </div>
    <van-popup
      v-model:show="isGenerating"
      :close-on-click-overlay="false"
      style="--van-popup-background: transparent"
    >
      <div
        class="loading-bg-white p-24px rounded-16px flex flex-col gap-12px items-center justify-center backdrop-blur-24px"
      >
        <van-loading type="circular" color="#0B8A1B" />
        <div
          v-if="type === 'image' && adminConfig?.allowPrint !== false"
          class="text-14px text-black"
        >
          {{ $t("status.imageGenerating", { time: 30 }) }}
        </div>
        <div v-else class="text-14px text-black">
          <span v-html="$t('status.generating', { time: 3 })" />
        </div>
        <van-button
          type="default"
          class="text-16px font-500 px-16px h-36px !text-14px !mt-10px !rounded-8px !border-none !bg-#09090A0A"
          @click="handleCancel"
        >
          {{ $t("actions.cancel") }}
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style lang="less">
.creation-container {
  background-color: #9ce5fa;
  background-image: url(https://ali.a.yximgs.com/kos/nlav12119/JSsSVYUG_2025-07-15-20-17-58.png);
  background-size: cover;

  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  -webkit-text-size-adjust: 100%; /* Safari/iOS 兼容 */
  text-size-adjust: 100%; /* 标准写法 */

  // 校招活动主题样式
  &.xiaozhao-theme {
    background-image: url(https://tx.a.yximgs.com/kos/nlav12119/SNqERzGo_2025-08-25-22-38-23.png);

    .generate-btn {
      background: linear-gradient(99deg, #ff4906 0.35%, #fea623 100.35%);

      .van-button__content {
        .van-button__text {
          color: #fff;
        }
      }
    }

    .warning-tip {
      color: var(--color-text-5);
    }
  }

  // TODO: 行内的class不生效，所以
  .blur-bg {
    filter: blur(20px);
    -webkit-filter: blur(20px);
  }
  .van-uploader__input-wrapper {
    display: block !important;
  }
  .upload-area {
    background: linear-gradient(147.61deg, #313a47 0%, #171a1f 100%);
  }
  .generate-btn {
    font-family: "Alimama ShuHeiTi";
    background: linear-gradient(
      98.88deg,
      #f7ffe0 0.35%,
      #74ff52 50.35%,
      #1bf6fd 100.35%
    );
    border: 4px solid black;
    .van-button__content {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 4px;
      .van-button__text {
        line-height: 28px;
        height: 28px;
      }
    }
  }
  .result-section-bg {
    background: rgba(255, 255, 255, 0.64);
    backdrop-filter: blur(20px);
  }

  .print-status-display {
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;

    &.bg-green-100 {
      background: rgba(76, 175, 80, 0.1);
      border-color: rgba(76, 175, 80, 0.3);
    }

    &.bg-red-100 {
      background: rgba(244, 67, 54, 0.1);
      border-color: rgba(244, 67, 54, 0.3);
    }

    &.bg-blue-100 {
      background: rgba(33, 150, 243, 0.1);
      border-color: rgba(33, 150, 243, 0.3);
    }

    &.bg-yellow-100 {
      background: rgba(255, 193, 7, 0.1);
      border-color: rgba(255, 193, 7, 0.3);
    }
  }
}

// 动画定义
.animate-slideUp {
  animation: slideUp 0.8s cubic-bezier(0.22, 0.61, 0.36, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(100px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.preview-result {
  max-height: 50vh;
  object-fit: contain;
}
// fuck 荣耀手机
.upload-bg-white {
  background: white;
}
.upload-text-white {
  color: white !important;
}
.btn-bg {
  background: #09090a0a;
  &:disabled {
    background: 09090A0a;
  }
}
// fuck iPhone 13Pro max
.loading-bg-white {
  background: #f9fbfc;
}
</style>
