<template>
  <div
    id="h5App"
    class="creation-container h-100vh min-h-100vh relative overflow-hidden flex flex-col items-center"
  >
    <div
      v-show="!generatedResult"
      class="w-full mt-28px flex justify-center mb-24px"
    >
      <img
        v-if="type === 'image'"
        class="w-329px h-156px"
        src="https://ali.a.yximgs.com/kos/nlav12119/LMmTMlSW_2025-07-11-14-27-35.png"
        alt=""
      />
      <img
        v-else
        class="w-329px h-156px"
        src="https://tx.a.yximgs.com/kos/nlav12119/nJaiWFln_2025-07-11-17-25-24.png"
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
          class="blur-40px absolute left-0 top-0 w-full h-full"
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
          class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px color-black bg-#09090A0A"
          @click="handleReplace()"
        >
          <IconSvg name="replace" color="black" />
          替换
        </button>
        <button
          class="h-full flex-1 rounded-8px flex items-center justify-center gap-6px color-black bg-#09090A0A"
          @click="handleDelete"
        >
          <IconSvg name="delete" color="black" />
          删除
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
        <template #default v-if="!isGenerating">
          <van-icon name="magic" /> 立即生成{{
            type === "image" ? "图片" : "视频"
          }}
        </template>
      </van-button>
      <div
        class="warning-tip mt-20px text-12px text-white flex items-center justify-center gap-4px"
      >
        <IconSvg name="inform" :size="14" color="#fff" />
        内容由AI生成，禁止利用功能从事违法活动。
      </div>
    </div>

    <!-- 生成的结果区域 -->
    <div
      v-if="generatedResult"
      class="result-section-bg absolute left-0 top-0 right-0 bottom-0"
    ></div>
    <div
      v-if="generatedResult"
      class="result-section w-full box-border px-18px pt-40px flex flex-col items-center relative z-10 animate-slideUp"
    >
      <div class="rounded-8px">
        <img
          v-if="type === 'image'"
          :src="generatedResult"
          alt="生成的图片"
          class="w-380px h-570px object-cover object-center rounded-8px shadow-sm"
        />
        <video
          v-else
          :src="generatedResult"
          controls
          autoplay
          preload="auto"
          class="w-380px h-570px object-cover object-center rounded-8px shadow-sm"
        ></video>
      </div>

      <div class="result-actions w-full h-48px flex gap-8px mt-24px">
        <van-button
          icon="revoke"
          type="default"
          @click="backToEdit"
          class="action-btn flex-1 h-full text-16px font-500 !rounded-8px !border-none shadow-sm !text-14px"
        >
          返回
        </van-button>
        <van-button
          v-if="type === 'image'"
          icon="print"
          type="default"
          @click="printImage"
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
      :title="`保存${type === 'image' ? '图片' : '视频'}指导`"
      show-cancel-button
      @confirm="handleSaveGuideConfirm"
    >
      <div class="save-guide p-15px text-center">
        <p>
          长按下方{{ type === "image" ? "图片" : "视频" }}，选择"保存{{
            type === "image" ? "图片" : "视频"
          }}"
        </p>
        <img
          v-if="type === 'image'"
          :src="generatedResult"
          alt="保存指导"
          class="guide-image w-full max-h-300px object-contain my-15px rounded-8px border border-gray-200"
        />
        <video
          v-else
          :src="generatedResult"
          controls
          class="guide-video w-full max-h-300px object-contain my-15px rounded-8px border border-gray-200"
        ></video>
        <p class="save-tip text-12px text-red">
          如无法保存，请使用浏览器打开本页面
        </p>
      </div>
    </van-dialog>

    <!-- 加载层 -->
    <van-overlay
      :show="isLoading"
      class="overlay flex items-center justify-center z-2000"
    >
      <div
        class="loading-wrapper text-center bg-white/95 py-30px px-40px rounded-16px shadow-lg"
      >
        <van-loading type="spinner" size="48" color="#1989fa" />
        <p class="loading-text mt-20px text-16px text-gray-800 font-500">
          {{ loadingText }}
        </p>
      </div>
    </van-overlay>
  </div>
</template>

<script lang="ts" setup>
import { showToast } from "vant";
import useCreation, { type CreationType } from "@/composables/useCreation";

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
  loadingText,
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

// 模拟生成API
const mockGenerate = (file: string, type: CreationType): Promise<string> => {
  return new Promise((resolve, reject) => {
    // 模拟30%的失败率
    const shouldFail = Math.random() < 0.3;

    setTimeout(
      () => {
        if (shouldFail) {
          reject(new Error("模拟API失败"));
          return;
        }

        if (type === "image") {
          // 图片处理逻辑
          const canvas = document.createElement("canvas");
          const ctx = canvas.getContext("2d")!;
          const img = new Image();
          img.src = file;
          img.onload = () => {
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);

            // 添加艺术效果
            const imageData = ctx.getImageData(
              0,
              0,
              canvas.width,
              canvas.height
            );
            const data = imageData.data;

            // 添加暖色滤镜
            for (let i = 0; i < data.length; i += 4) {
              data[i] = Math.min(255, data[i] * 1.2); // R
              data[i + 1] = Math.min(255, data[i + 1] * 1.1); // G
              data[i + 2] = data[i + 2] * 0.9; // B
            }

            ctx.putImageData(imageData, 0, 0);

            // 添加文字水印
            ctx.font = "bold 40px Arial";
            ctx.fillStyle = "rgba(255, 255, 255, 0.6)";
            ctx.textAlign = "center";
            ctx.fillText("AI创作", canvas.width / 2, canvas.height - 60);

            resolve(canvas.toDataURL("image/jpeg"));
          };
        } else {
          // 视频类型 - 这里应该调用真实API生成视频
          // 由于在客户端无法生成视频，我们模拟返回一个视频URL
          // 实际项目中应该替换为真实API调用
          resolve(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
          );
        }
      },
      type.value === "image" ? 2000 : 5000
    ); // 视频生成时间更长
  });
};

// 处理生成
const handleGenerate = async () => {
  // TODO: 禁用态判断
  if (isGenerating.value) {
    return;
  }
  if (!uploadedImage.value) {
    showToast("请先上传图片");
  }
  await generate(mockGenerate);
};

// 处理保存
const handleSave = () => {
  if (type.value === "image") {
    save("创作图片", "jpg");
  } else {
    save("创作视频", "mp4");
  }
};

// 设置文件大小限制
onMounted(() => {
  // 可以根据类型设置不同的限制
  maxFileSize.value = 10 * 1024 * 1024; // 10MB
});
</script>

<style lang="less">
.creation-container {
  background-color: #9ce5fa;
  background-image: url(https://tx.a.yximgs.com/kos/nlav12119/naVetQFu_2025-07-11-14-31-25.png);
  background-size: cover;

  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  .upload-area {
    background: linear-gradient(147.61deg, #313a47 0%, #171a1f 100%);
  }
  .generate-btn {
    background: linear-gradient(
      98.88deg,
      #f7ffe0 0.35%,
      #74ff52 50.35%,
      #1bf6fd 100.35%
    );
    border: 4px solid black;
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
