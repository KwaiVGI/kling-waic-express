<template>
  <div
    id="h5App"
    class="creation-container min-h-100vh relative overflow-hidden"
  >
    <!-- 背景模糊层 -->
    <div
      v-if="generatedResult"
      class="blur-background absolute top-0 left-0 w-full h-full z--1"
      :style="blurStyle"
    ></div>

    <!-- 顶部导航栏 -->
    <van-nav-bar
      :title="title"
      left-arrow
      @click-left="$router.go(-1)"
      placeholder
      fixed
    />

    <div
      class="container max-w-500px mx-auto px-4 pt-60px pb-4 relative z-1"
      :class="{ 'transform translate-y-10%': generatedResult }"
    >
      <!-- 上传区域 -->
      <div
        v-show="!uploadedImage"
        class="upload-section bg-white/92 rounded-16px shadow-sm py-30px px-20px mt-20px text-center"
      >
        <div class="upload-tips mb-20px">
          <p class="tip-main text-18px font-bold text-blue">
            {{ uploadTitle }}
          </p>
          <p class="tip-sub text-14px text-gray-600 mt-1">建议使用人像照片</p>
          <p class="tip-sub text-14px text-gray-600">
            主体明确、轮廓清晰的照片效果最佳
          </p>
        </div>
        <van-uploader
          ref="uploaderRef"
          v-model="fileList"
          :after-read="handleUpload"
          :max-count="1"
          reupload
          :preview-image="false"
          accept="image/*"
          capture="camera"
          :max-size="maxFileSize"
          @oversize="onOversize"
        >
          <div
            class="upload-area bg-blue-50/80 border-2 border-dashed border-blue-100 rounded-12px py-40px px-20px text-blue transition-all active:bg-blue-100/80 active:scale-98"
          >
            <van-icon name="photograph" size="48" color="#1989fa" />
            <p class="mt-12px text-16px font-500 text-gray-800">
              点击拍照或选择照片
            </p>
            <p class="upload-tip mt-8px text-12px text-gray-500">
              支持JPG、PNG格式，大小不超过{{ maxFileSizeMB }}MB
            </p>
          </div>
        </van-uploader>
      </div>

      <!-- 图片预览区域 -->
      <div
        v-if="uploadedImage && !generatedResult"
        class="preview-section bg-white/92 rounded-16px shadow-sm py-20px px-20px mt-20px"
      >
        <div
          class="image-card rounded-12px overflow-hidden bg-gray-50 border border-gray-200"
        >
          <div class="image-container p-15px bg-blue-50">
            <img
              :src="uploadedImage"
              alt="上传的图片"
              @click="openPreview(uploadedImage)"
              class="preview-image w-full max-h-50vh object-contain rounded-8px shadow-sm"
            />
          </div>

          <div class="image-actions flex gap-12px p-15px bg-white">
            <van-button
              icon="delete-o"
              type="danger"
              size="small"
              @click="handleDelete"
              plain
            >
              删除
            </van-button>
            <van-button
              icon="replay"
              type="primary"
              size="small"
              @click="handleReplace()"
              plain
            >
              替换
            </van-button>
          </div>
        </div>
      </div>

      <!-- 生成按钮区域 -->
      <div v-if="!generatedResult" class="generate-section mt-25px text-center">
        <van-button
          round
          block
          type="primary"
          @click="handleGenerate"
          :loading="isGenerating"
          :disabled="!uploadedImage || isGenerating"
          class="generate-btn h-50px text-18px font-bold shadow-md bg-blue/90 border-none text-white disabled:bg-gray-300 disabled:shadow-none"
        >
          <template #default v-if="!isGenerating">
            <van-icon name="magic" /> 立即生成{{
              type === "image" ? "图片" : "视频"
            }}
          </template>
          <template #loading> 生成中，请稍后... </template>
        </van-button>
        <p
          class="warning-tip mt-12px text-12px text-orange flex items-center justify-center gap-4px"
        >
          <van-icon name="warning-o" color="#ff976a" />
          内容由AI生成，禁止利用功能从事违法活动。
        </p>
      </div>

      <!-- 生成的结果区域 -->
      <div
        v-if="generatedResult"
        class="result-section bg-white/92 rounded-24px py-30px px-20px mt-25px shadow-lg backdrop-blur-10px border border-white/50 animate-slideUp"
      >
        <div
          class="result-header flex items-center justify-center gap-8px text-20px font-bold text-green mb-20px"
        >
          <van-icon name="success" color="#07c160" />
          <span>创作完成</span>
        </div>

        <div
          class="result-container p-15px bg-blue-50/80 rounded-16px border border-blue-100/50 shadow-sm"
        >
          <img
            v-if="type === 'image'"
            :src="generatedResult"
            alt="生成的图片"
            @click="openPreview(generatedResult)"
            class="preview-result w-full max-h-50vh object-contain rounded-8px shadow-sm"
          />
          <video
            v-else
            :src="generatedResult"
            controls
            @click="openPreview(generatedResult)"
            class="preview-result w-full max-h-50vh object-contain rounded-8px shadow-sm"
          ></video>
        </div>

        <div class="result-actions flex gap-12px mt-30px">
          <van-button
            icon="revoke"
            type="default"
            @click="backToEdit"
            class="action-btn h-50px text-16px font-500 rounded-12px shadow-sm"
          >
            返回编辑
          </van-button>

          <van-button
            icon="down"
            type="primary"
            @click="handleSave"
            :loading="isSaving"
            class="action-btn h-50px text-16px font-500 rounded-12px shadow-sm"
          >
            保存{{ type === "image" ? "图片" : "视频" }}
          </van-button>

          <van-button
            v-if="type === 'image'"
            icon="print"
            type="info"
            @click="printImage"
            :loading="isPrinting"
            class="action-btn h-50px text-16px font-500 rounded-12px shadow-sm"
          >
            打印图片
          </van-button>
        </div>
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
  blurStyle,

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

// 根据类型设置UI文本
const title = computed(() =>
  type.value === "image" ? "AI图片创作" : "AI视频创作"
);

const uploadTitle = computed(() =>
  type.value === "image"
    ? "上传1张照片，生成你的专属转绘画报~"
    : "上传1张照片，生成你的专属AI视频~"
);

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
            "https://example.com/generated-video.mp4?image=" +
              encodeURIComponent(file)
          );
        }
      },
      type.value === "image" ? 2000 : 5000
    ); // 视频生成时间更长
  });
};

// 处理生成
const handleGenerate = async () => {
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
  background: linear-gradient(135deg, #f8f9ff 0%, #e8f4ff 100%);
  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;

  .blur-background {
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    transform: scale(1.1);
    opacity: 0.7;
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

.preview-image,
.preview-result {
  max-height: 50vh;
  object-fit: contain;
}

// 响应式调整
@media (max-width: 400px) {
  .container {
    padding: 12px;
    padding-top: 56px;
  }

  .upload-section {
    padding: 20px 15px;
  }

  .result-actions {
    flex-direction: column;
    gap: 10px;

    .action-btn {
      width: 100%;
    }
  }
}
</style>
