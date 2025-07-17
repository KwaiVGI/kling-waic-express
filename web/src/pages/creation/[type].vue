<template>
  <div
    ref="containerRef"
    id="h5App"
    class="creation-container h-100vh relative overflow-hidden flex flex-col items-center"
  >
    <div
      ref="step1Ref"
      :style="{ zoom: step1Zoom }"
      class="step-1 w-full flex flex-col items-center pb-28px"
    >
      <div
        v-show="!generatedResult"
        class="w-full mt-28px flex flex-col items-center mb-36px"
      >
        <img
          class="w-106px h-32px mb-22px ml-20px self-start"
          src="https://ali.a.yximgs.com/kos/nlav12119/QBZriaHi_2025-07-15-20-14-14.png"
          alt=""
        />
        <img
          v-if="type === 'image'"
          class="w-329px h-126px"
          src="https://ali.a.yximgs.com/kos/nlav12119/sZscckOe_2025-07-15-20-11-41.png"
          alt=""
        />
        <img
          v-else
          class="w-329px h-126px"
          src="https://tx.a.yximgs.com/kos/nlav12119/HosJbOVv_2025-07-15-20-12-01.png"
          alt=""
        />
      </div>
      <!-- 上传区域 -->
      <div
        v-show="!uploadedImage"
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
            <p class="mt-10px text-14px text-white">上传照片</p>
            <p class="upload-tip mt-4px text-12px text-#B0B4B8">
              支持JPG、PNG格式，大小不超过{{ maxFileSizeMB }}MB
            </p>
          </div>
        </van-uploader>
        <div
          v-if="type === 'image'"
          class="absolute bottom-0 left-0 w-360px h-124px"
        >
          <img
            class="w-full h-full"
            src="https://tx.a.yximgs.com/kos/nlav12119/egmRUScU_2025-07-11-17-22-10.png"
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
            class="h-254px object-cover object-center relative z-10"
            :class="type === 'image' ? 'w-169px' : 'w-143px'"
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
            <span> 替换 </span>
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
            <span> 删除 </span>
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
          立即生成
        </van-button>
        <div
          class="warning-tip mt-20px text-12px text-black flex items-center justify-center gap-4px"
        >
          <IconSvg name="inform" :size="14" color="#000" />
          内容由AI生成，禁止利用功能从事违法活动。
        </div>
      </div>
    </div>
    <!-- 生成的结果区域 -->
    <div
      v-show="generatedResult"
      class="result-section-bg absolute left-0 top-0 right-0 bottom-0"
    ></div>
    <div
      v-show="generatedResult"
      ref="step2Ref"
      :style="{ zoom: step2Zoom }"
      class="result-section w-full box-border px-18px py-40px flex flex-col items-center relative z-10"
      :class="{ 'animate-slideUp': isGuided }"
    >
      <div class="w-380px h-570px rounded-8px relative">
        <img
          v-if="type === 'image'"
          :src="generatedResult"
          alt="生成的图片"
          class="w-full h-full object-cover object-center rounded-8px shadow-sm"
        />
        <video
          v-else
          :src="generatedResult"
          controls
          autoplay
          preload="auto"
          class="w-full h-full object-cover object-center rounded-8px shadow-sm"
        ></video>
        <div
          id="guideNo"
          class="w-100px h-26px absolute right-10px bottom-10px"
        ></div>
      </div>

      <div class="result-actions w-full h-48px flex gap-8px mt-24px">
        <van-button
          icon="revoke"
          type="default"
          @click="backToEdit"
          class="action-btn w-33% h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          返回
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
            打印图片
          </van-button>

          <van-button
            icon="down"
            type="primary"
            @click="handleSave"
            :loading="isSaving"
            class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px !bg-#0B8A1B"
          >
            保存{{ type === "image" ? "图片" : "视频" }}
          </van-button>
        </div>
      </div>
      <div
        class="warning-tip mt-20px text-12px text-#5E6266ff flex items-center justify-center gap-4px"
      >
        <IconSvg name="inform" :size="14" color="#5E6266ff" />
        内容由AI生成，禁止利用功能从事违法活动。
      </div>
    </div>

    <!-- 预览弹窗 -->
    <van-image-preview
      v-if="type === 'image'"
      v-model:show="showPreview"
      :images="previewItems"
      :start-position="previewIndex"
    />

    <van-popup
      v-else
      v-model:show="showPreview"
      round
      position="bottom"
      :style="{ height: '80%' }"
    >
      <video
        :src="previewItems[previewIndex]"
        controls
        autoplay
        class="w-full h-full"
      ></video>
    </van-popup>

    <!-- 保存指导弹窗 -->
    <van-dialog
      v-model:show="showSaveGuide"
      :width="vw(368)"
      show-cancel-button
      @confirm="handleSaveGuideConfirm"
    >
      <template #title>
        <p class="text-center text-20px font-bold">
          {{ type === "image" ? "图片" : "视频" }}保存
        </p>
      </template>
      <p class="p-14px text-center text-black mb-24px">
        长按下方{{ type === "image" ? "图片" : "视频" }}，选择"保存{{
          type === "image" ? "图片" : "视频"
        }}"
      </p>
      <div class="mx-24px h-254px relative rounded-12px overflow-hidden">
        <div
          v-if="type === 'image'"
          class="blur-bg blur-20px absolute left-0 top-0 w-full h-full bg-cover bg-center"
          :style="{ backgroundImage: `url(${generatedResult})` }"
        ></div>
        <img
          v-if="type === 'image'"
          :src="generatedResult"
          alt="保存指导"
          class="guide-image w-full h-full object-contain relative z-10"
        />
        <video
          v-else
          :src="generatedResult"
          controls
          class="guide-video w-full h-full object-contain relative z-10"
        ></video>
      </div>
      <template #footer>
        <div class="flex flex-col gap-24px mt-16px px-24px pb-24px box-border">
          <div class="flex items-center gap-8px">
            <!-- <van-button
              type="default"
              @click="showSaveGuide = false"
              class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none color-black !bg-#09090A0A !text-14px"
            >
              取消
            </van-button> -->

            <van-button
              type="primary"
              @click="showSaveGuide = false"
              :loading="isSaving"
              round
              class="action-btn flex-1 h-full text-16px font-500 !border-none shadow-sm !text-14px !bg-#0B8A1B"
            >
              我知道了
            </van-button>
          </div>
          <p
            class="text-13px text-#EB9109ff leading-1 text-center flex justify-center gap-4px items-center"
          >
            <IconSvg name="inform" :size="14" /><span
              >如无法保存，请使用浏览器打开本页面</span
            >
          </p>
        </div>
      </template>
    </van-dialog>
    <guide-overlay
      v-if="showGuide"
      v-model="showGuide"
      :guides="currentGuides"
      :theme="currentTheme"
      @finish="onFinishGuide"
    />
  </div>
</template>

<script lang="ts" setup>
import { showToast } from "vant";
import useCreation, { type CreationType } from "@/composables/useCreation";
import { getTaskStatus, newTask } from "@/api/creation";
import { STORAGE_TOKEN_KEY, STORAGE_GUIDE_KEY } from "@/stores/mutation-type";
import vw from "@/utils/inline-px-to-vw";
import { useZoom } from "@/composables/useZoom";
import { useGuide } from "@/composables/useGuide";

const route = useRoute();

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
  maxFileSizeMB,
  fileList,
  uploadedImage,
  generatedResult,
  isLoading,
  isGenerating,
  isSaving,
  isPrinting,
  showPreview,
  previewItems,
  previewIndex,
  showSaveGuide,
  handleUpload,
  onOversize,
  openPreview,
  handleDelete,
  handleReplace,
  generate,
  save,
  backToEdit,
  handleSaveGuideConfirm,
  printImage,
} = useCreation(type.value as "image" | "video");

const isGuided = ref(!!localStorage.getItem(STORAGE_GUIDE_KEY));
const { currentGuides, showGuide, startGuide, currentTheme, finishGuide } =
  useGuide();

const wait = async (delay: number) =>
  new Promise((resolve) => setTimeout(resolve, delay));

const currentImageNo = ref("");

// 生成
const doGenerate = async (file: File, type: CreationType): Promise<string> => {
  // mock
  // if (type === "image") {
  //   return "https://s2-111386.kwimgs.com/bs2/mmu-kolors-public/frontgallery-20240514-33.png?x-oss-process=image/resize,m_mfit,w_1000";
  // } else {
  //   return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
  // }
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
      });

      status = result.status;
      const { url } = result.outputs || {};

      if (status === "SUCCEED") {
        if (url) {
          currentImageNo.value = result.name;
          return url; // 成功返回URL
        }
        throw new Error("任务完成但未返回有效URL");
      }

      if (status === "FAILED") {
        throw new Error("任务处理失败");
      }

      // 如果未完成，等待一段时间再继续
      await wait(delay);
    }

    throw new Error("任务处理超时");
  } catch (error) {
    console.error("生成过程中出错:", error);
    throw error; // 重新抛出错误，让调用者处理
  }
};

// 处理生成
const handleGenerate = async () => {
  // TODO: 禁用态判断
  if (isGenerating.value) {
    return;
  }
  if (!uploadedImage.value) {
    showToast("请先上传图片");
    return;
  }

  await generate(doGenerate);
  if (isGuided.value || type.value !== "image") {
    return;
  }
  await wait(0);
  // 启动引导
  startGuide([
    {
      element: "#guideNo",
      image:
        "https://tx.a.yximgs.com/kos/nlav12119/wKJuuNBr_2025-07-16-20-04-31.png",
      position: "top",
    },
    {
      element: "#guideYes",
      image:
        "https://tx.a.yximgs.com/kos/nlav12119/XkqhokRT_2025-07-16-20-05-16.png",
      position: "top",
    },
  ]);
};
const onFinishGuide = () => {
  isGuided.value = true;
  localStorage.setItem(STORAGE_GUIDE_KEY, "1");
  finishGuide();
};

// 处理保存
const handleSave = () => {
  if (type.value === "image") {
    save("创作图片", "jpg");
  } else {
    save("创作视频", "mp4");
  }
};

const step1Ref = ref<HTMLElement | null>(null);
const step2Ref = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const step1Zoom = useZoom(step1Ref, containerRef);
const step2Zoom = useZoom(step2Ref, containerRef);

// 设置文件大小限制
onMounted(() => {
  localStorage.setItem(STORAGE_TOKEN_KEY, route.query.token as string);
  console.log({ userAgent: navigator.userAgent });
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
