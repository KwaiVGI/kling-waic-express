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
        class="w-full mt-28px flex flex-col items-center mb-36px relative"
      >
        <img
          class="w-106px h-32px mb-22px ml-20px self-start"
          src="https://ali.a.yximgs.com/kos/nlav12119/QBZriaHi_2025-07-15-20-14-14.png"
          alt=""
        />
        <img
          v-if="type === 'image'"
          class="w-329px h-126px"
          src="https://tx.a.yximgs.com/kos/nlav12119/EghFVodo_2025-07-21-20-42-24.png"
          alt=""
        />
        <img
          v-else
          class="w-329px h-126px"
          src="https://tx.a.yximgs.com/kos/nlav12119/xbwHVLkr_2025-07-21-12-02-53.png"
          alt=""
        />
        <LangSwitcher class="absolute right-20px top-0" />
      </div>
      <!-- 上传区域 -->
      <div
        v-show="!uploadedImage && !generatedResult"
        class="w-360px h-360px box-border bg-white rounded-24px p-20px text-center relative"
      >
        <van-uploader
          ref="uploaderRef"
          v-model="fileList"
          :after-read="handleUpload"
          :max-count="1"
          reupload
          :preview-image="false"
          accept="image/*"
          :max-size="maxFileSize"
          @oversize="onOversize"
        >
          <div
            class="upload-area rounded-12px w-320px flex flex-col items-center justify-center"
            :class="type === 'image' ? 'h-254px' : 'h-320px'"
          >
            <IconSvg name="add-image" :size="32" />
            <p class="mt-10px text-14px text-white">
              {{ $t("actions.uploadPhoto") }}
            </p>
          </div>
        </van-uploader>
        <div
          v-if="type === 'image'"
          class="absolute bottom-0 left-0 w-360px h-124px"
        >
          <img
            class="w-full h-full"
            src="https://tx.a.yximgs.com/kos/nlav12119/WmkviEwP_2025-07-21-20-40-09.png"
            alt=""
          />
        </div>
      </div>

      <!-- 图片预览区域 -->
      <div
        v-if="uploadedImage && !generatedResult"
        class="w-360px h-360px box-border bg-white rounded-24px p-20px text-center relative"
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
            class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px leading-1 text-black bg-#09090A0A disabled:bg-#09090A0a disabled:text-#B0B4B8ff"
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
            class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px leading-1 color-black bg-#09090A0A disabled:bg-#09090A0a disabled:text-#B0B4B8ff"
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
          loading-text="生成中，请稍后..."
          class="generate-btn !w-320px !h-56px !text-20px font-bold !text-black"
        >
          {{ $t("actions.generateNow") }}
        </van-button>
        <div
          class="warning-tip mt-20px text-12px text-black flex items-center justify-center gap-4px"
        >
          <IconSvg name="inform" :size="14" color="#000" />
          {{ $t("descriptions.aiDisclaimer") }}
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
          class="w-full h-full object-cover overflow-hidden object-center rounded-8px shadow-sm"
        ></VideoPlayer>
      </div>

      <div
        class="result-actions h-48px flex gap-8px mt-24px"
        :class="type === 'image' ? 'w-full' : 'w-340px'"
      >
        <van-button
          type="default"
          @click="backToEdit"
          class="action-btn w-33% h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          {{ $t("actions.back") }}
        </van-button>
        <div id="guideYes" class="flex h-full gap-8px w-66%">
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
            type="primary"
            @click="handleSave"
            :loading="isSaving"
            class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px !bg-#0B8A1B"
          >
            {{ $t("actions.save") }}
          </van-button>
        </div>
      </div>
      <div
        class="warning-tip mt-20px text-12px text-#5E6266ff flex items-center justify-center gap-4px"
      >
        <IconSvg name="inform" :size="14" color="#5E6266ff" />
        {{ $t("descriptions.aiDisclaimer") }}
      </div>
    </div>
    <van-popup
      v-model:show="isGenerating"
      style="--van-popup-background: transparent"
    >
      <div
        class="flex flex-col justify-center items-center bg-#000000cc rounded-8px p-20px"
      >
        <van-loading type="circular" color="#74FF52ff" />
        <div v-if="type === 'image'" class="mt-8px text-14px text-#B0B4B8ff">
          {{ $t("status.processing") }}
        </div>
        <div v-else class="mt-8px text-14px text-#B0B4B8ff">
          <span v-html="$t('status.generating', { time: 3 })"></span>
        </div>
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
// import { useGuide } from "@/composables/useGuide";

const route = useRoute();
const { t } = useI18n();

// 从路由参数获取类型
const type = ref<string>(route.params.type as CreationType);
if (type.value !== "image" && type.value !== "video") {
  showToast("不支持的创作类型");
  type.value = "image";
}

// 使用组合函数
const {
  uploaderRef,
  maxFileSize,
  fileList,
  uploadedImage,
  generatedResult,
  isGenerating,
  isSaving,
  isPrinting,
  handleUpload,
  onOversize,
  openPreview,
  handleDelete,
  handleReplace,
  generate,
  save,
  backToEdit,
  printImage,
} = useCreation(type.value as "image" | "video");

// const isGuided = ref(!!localStorage.getItem(STORAGE_GUIDE_KEY));
// const { currentGuides, showGuide, startGuide, finishGuide } = useGuide();

const wait = async (delay: number) =>
  new Promise((resolve) => setTimeout(resolve, delay));

const currentImageNo = ref("");

// 生成
const doGenerate = async (file: File, type: CreationType): Promise<string> => {
  try {
    // 1. 创建新任务
    const { name } = await newTask({
      file,
      type: type === "image" ? "STYLED_IMAGE" : "VIDEO_EFFECT",
    });

    // 2. 轮询任务状态
    let status = "";
    const maxAttempts = 1800; // 最大尝试次数，防止无限循环
    const delay = 2000; // 每次轮询间隔2秒

    await wait(10 * 1000);
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      // 获取任务状态
      const result = await getTaskStatus({
        name,
        type: type === "image" ? "STYLED_IMAGE" : "VIDEO_EFFECT",
        locale: locale.value === "en-US" ? "US" : "CN",
      });

      status = result.status;
      const { url } = result.outputs || {};

      if (status === "SUCCEED") {
        if (url) {
          currentImageNo.value = result.name;
          return url; // 成功返回URL
        }
        throw new Error();
      }

      if (status === "FAILED") {
        throw new Error();
      }

      // 如果未完成，等待一段时间再继续
      await wait(delay);
    }

    throw new Error();
  } catch (error) {
    console.error("生成过程中出错:", error);
    throw error; // 重新抛出错误，让调用者处理
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
    await generate(doGenerate);
  } catch (error) {
    if (error.message === "NO_HUMAN_DETECTED") {
      showToast(t("upload.noFaceDetected"));
    } else if (error.message === "LONG_QUEUES") {
      showToast(t("errors.api.quotaExceeded"));
    } else {
      showToast(t("upload.generationFailed"));
    }
    return;
  }

  // 将图片URL放到查询参数上
  history.pushState(
    null,
    "",
    `?token=${localStorage.getItem(
      STORAGE_USER_TOKEN_KEY
    )}&result=${encodeURIComponent(generatedResult.value)}`
  );
};
// 处理保存
const handleSave = () => {
  if (type.value === "image") {
    save("", "jpg");
  } else {
    save("", "mp4");
  }
};

const step1Ref = ref<HTMLElement | null>(null);
const step2Ref = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const step1Zoom = useZoom(step1Ref, containerRef);
const step2Zoom = useZoom(step2Ref, containerRef);

// 设置文件大小限制
onMounted(() => {
  if (route.query.token) {
    localStorage.setItem(STORAGE_USER_TOKEN_KEY, route.query.token as string);
  }
  console.log({ userAgent: navigator.userAgent });
  if (route.query.result) {
    generatedResult.value = decodeURIComponent(route.query.result as string);
  }
});
</script>

<style lang="less">
.creation-container {
  background-color: #9ce5fa;
  background-image: url(https://ali.a.yximgs.com/kos/nlav12119/JSsSVYUG_2025-07-15-20-17-58.png);
  background-size: cover;

  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  // TODO: 行内的class不生效，所以
  .blur-bg {
    filter: blur(20px);
    -webkit-filter: blur(20px);
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
</style>
