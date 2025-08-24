<template>
  <div
    ref="containerRef"
    id="h5App"
    class="creation-container !min-h-100vh relative flex flex-col items-center"
  >
    <div
      ref="step1Ref"
      :style="{ zoom: step1Zoom }"
      class="step-1 w-full flex flex-col items-center pb-28px"
    >
      <div
        v-show="!generatedResult"
        class="w-full mt-28px flex flex-col items-center mb-28px relative"
      >
        <img
          class="w-120px h-32px mb-22px ml-20px self-start"
          :src="assets.logo"
          alt=""
        />
        <img class="w-414px h-130px" :src="assets.banner" alt="" />
        <LangSwitcher
          class="absolute right-20px top-0"
          @change="onLocaleChange"
        />
      </div>
      <!-- 上传区域 -->
      <div
        v-show="(!uploadedImage && !generatedResult) || uploading"
        class="w-360px h-360px box-border upload-bg-white rounded-24px p-20px text-center relative"
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
            class="upload-area rounded-12px w-320px flex flex-col items-center justify-center"
            :class="type === 'image' ? 'h-254px' : 'h-320px'"
          >
            <div
              class="uploading w-full h-full flex flex-col items-center justify-center gap-12px"
              v-if="uploading"
            >
              <van-loading type="circular" color="#0B8A1B" />
              <div class="text-14px upload-text-white">
                {{ $t("upload.uploading") }}
              </div>
            </div>
            <template v-else>
              <IconSvg name="add-image" :size="32" />
              <p class="mt-10px text-14px upload-text-white">
                {{ $t("actions.uploadPhoto") }}
              </p>
            </template>
          </div>
        </van-uploader>
        <div
          v-if="type === 'image'"
          class="absolute bottom-0 left-0 w-360px h-124px"
        >
          <img class="w-full h-full" :src="assets.imageTip" alt="" />
        </div>
      </div>

      <!-- 图片预览区域 -->
      <div
        v-if="uploadedImage && !generatedResult && !uploading"
        class="w-360px h-360px box-border upload-bg-white rounded-24px p-20px text-center relative"
      >
        <div
          class="w-320px h-254px overflow-hidden rounded-12px flex items-center justify-center relative"
        >
          <div
            class="blur-bg blur-20px absolute left-0 top-0 w-full h-full bg-cover bg-center select"
            :style="{ backgroundImage: `url(${uploadedImage})` }"
          ></div>
          <img
            :src="uploadedImage"
            alt="上传的图片"
            @click="openPreview(uploadedImage)"
            class="h-full w-full object-contain object-center relative z-10"
          />
        </div>
        <div class="w-full h-48px flex justify-between gap-8px mt-16px">
          <button
            :disabled="isGenerating"
            class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px leading-1 text-black btn-bg disabled:text-#B0B4B8ff"
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
            class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px leading-1 color-black btn-bg disabled:text-#B0B4B8ff"
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
        class="generate-section mt-48px w-full text-center"
      >
        <van-button
          round
          type="primary"
          @click="handleGenerate"
          :loading="isGenerating"
          :loading-text="$t('status.processing')"
          class="generate-btn !w-320px !h-56px !text-20px font-bold !text-black"
        >
          {{ $t("actions.generateNow") }}
        </van-button>
        <div
          class="warning-tip mt-30px text-12px text-#5E6266ff flex flex-col items-center justify-center gap-8px"
        >
          <span>{{ $t("descriptions.aiDisclaimer") }}</span>
          <span>{{ screenTip }}</span>
        </div>
      </div>
    </div>
    <!-- 生成的结果区域 -->
    <div
      v-show="generatedResult"
      class="result-section-bg absolute left-0 top-0 right-0 bottom-0"
    ></div>
    <div
      v-if="generatedResult"
      ref="step2Ref"
      :style="{ zoom: step2Zoom }"
      class="result-section w-full box-border px-18px py-40px flex flex-col items-center relative z-10 animate-slideUp"
    >
      <div v-if="type === 'image'" class="w-380px h-570px rounded-8px relative">
        <img
          :src="generatedResult"
          alt="生成的图片"
          class="w-full h-full object-cover object-center rounded-8px shadow-sm"
        />
      </div>
      <div v-else class="w-340px h-604px rounded-8px relative">
        <VideoPlayer
          :src="generatedResult"
          :poster="uploadedImage"
          autoplay
          class="w-full h-full object-cover overflow-hidden object-center rounded-8px shadow-sm"
        ></VideoPlayer>
      </div>

      <div
        class="result-actions h-48px flex gap-8px mt-24px"
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
          v-if="type === 'image'"
          icon="print"
          type="default"
          @click="printImage(currentImageNo)"
          :loading="isPrinting"
          class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          {{ $t("actions.print") }}
        </van-button>

        <van-button
          type="default"
          @click="handleSave"
          :loading="isSaving"
          class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          {{ $t("actions.save") }}
        </van-button>
      </div>
      <div
        class="warning-tip mt-30px text-12px text-#5E6266ff flex flex-col items-center justify-center gap-8px"
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
        class="flex flex-col justify-center items-center loading-bg-white rounded-16px p-24px gap-12px backdrop-blur-24px"
      >
        <van-loading type="circular" color="#0B8A1B" />
        <div v-if="type === 'image'" class="text-14px text-black">
          {{ $t("status.processing") }}
        </div>
        <div v-else class="text-14px text-black">
          <span v-html="$t('status.generating', { time: 3 })"></span>
        </div>
        <van-button
          type="default"
          @click="handleCancel"
          class="!mt-10px px-16px h-36px text-16px font-500 !rounded-8px !border-none !text-14px !bg-#09090A0A"
        >
          {{ $t("actions.cancel") }}
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<script lang="ts" setup>
import { showToast } from "vant";
import useCreation, { type CreationType } from "@/composables/useCreation";
import { getTaskStatus, newTask } from "@/api/creation";
import { STORAGE_USER_TOKEN_KEY } from "@/stores/mutation-type";
import { useZoom } from "@/composables/useZoom";
import { updateQueryParams } from "@/utils/url";
import { waitWithAbort } from "@/utils/time";
import { locale, type Locale } from "@/utils/i18n";
import {
  bannerImageEn,
  bannerImageZh,
  bannerVideoEn,
  bannerVideoZh,
  logoEn,
  logoZh,
  imageTipEn,
  imageTipZh,
} from "./const";

const route = useRoute();
const { t } = useI18n();
// 从路由参数获取类型
const type = ref<string>((route.params.type as CreationType) || "image");

const assets = computed(() => {
  const isZh = locale.value === "zh-CN";
  const bannerImage = isZh ? bannerImageZh : bannerImageEn;
  const bannerVideo = isZh ? bannerVideoZh : bannerVideoEn;
  return {
    logo: isZh ? logoZh : logoEn,
    banner: type.value === "image" ? bannerImage : bannerVideo,
    imageTip: isZh ? imageTipZh : imageTipEn,
  };
});

const screenTip = computed(() => {
  return type.value === "image"
    ? t("descriptions.imageScreenTip")
    : t("descriptions.videoScreenTip");
});

// 使用组合函数
const {
  uploaderRef,
  fileList,
  uploadedImage,
  generatedResult,
  isGenerating,
  isSaving,
  isPrinting,
  handleUpload,
  uploading,
  openPreview,
  handleDelete,
  handleReplace,
  generate,
  save,
  backToEdit,
  printImage,
} = useCreation(type.value as "image" | "video");

const currentImageNo = ref("");
const sourceImageUrl = ref("");

// 生成
const doGenerate = async (
  url: string,
  type: CreationType,
  signal?: AbortSignal
): Promise<string> => {
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
};

let controller: AbortController | null = null;

const handleCancel = () => {
  if (controller) {
    controller.abort();
  }
};

// 处理生成
const handleGenerate = async () => {
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
};

// 处理保存
const handleSave = () => {
  if (type.value === "image") {
    save("", "jpg");
  } else {
    save("", "mp4");
  }
};

const onLocaleChange = (l: Locale) => {
  updateQueryParams(
    {
      lang: l,
    },
    "replace"
  );
};

const step1Ref = ref<HTMLElement | null>(null);
const step2Ref = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const step1Zoom = useZoom(step1Ref);
const step2Zoom = useZoom(step2Ref);

onMounted(() => {
  if (route.query.token) {
    localStorage.setItem(STORAGE_USER_TOKEN_KEY, route.query.token as string);
  }
  console.log({ userAgent: navigator.userAgent });
  if (route.query.resultUrl) {
    generatedResult.value = decodeURIComponent(route.query.resultUrl as string);
  }
  if (route.query.sourceUrl) {
    uploadedImage.value = decodeURIComponent(route.query.sourceUrl as string);
    sourceImageUrl.value = decodeURIComponent(route.query.sourceUrl as string);
  }
});
</script>

<style lang="less">
.creation-container {
  background-color: #9ce5fa;
  background-image: url(https://ali.a.yximgs.com/kos/nlav12119/JSsSVYUG_2025-07-15-20-17-58.png);
  background-size: cover;

  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  -webkit-text-size-adjust: 100%; /* Safari/iOS 兼容 */
  text-size-adjust: 100%; /* 标准写法 */
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
