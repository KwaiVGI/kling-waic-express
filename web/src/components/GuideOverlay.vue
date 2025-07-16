<template>
  <transition name="fade">
    <div class="guide-overlay" v-if="showGuide">
      <div class="guide-mask"></div>
      <div class="highlight-box" :style="{ ...highlightStyle }"></div>

      <div class="tooltip-container" :style="{ ...tooltipStyle }">
        <div class="tooltip-content">
          <div class="tooltip-image" v-if="currentGuide.image">
            <img
              class="w-full object-contain"
              :src="currentGuide.image"
              alt=""
            />
          </div>
        </div>
      </div>
      <div class="tooltip-footer">
        <!-- <van-button v-if="currentStep > 0" plain size="small" @click="prevStep"
          >上一步</van-button
        > -->
        <van-button
          v-if="currentStep < guides.length - 1"
          class="step-btn"
          @click="nextStep"
          >下一步</van-button
        >
        <van-button v-else class="step-btn" @click="finishGuide"
          >我知道了</van-button
        >
      </div>
    </div>
  </transition>
</template>

<script lang="ts" setup>
const props = defineProps<{
  guides: any[];
  theme: any;
}>();
const emit = defineEmits(["finish"]);
const showGuide = defineModel({ type: Boolean, default: false });
const currentStep = ref(0);

// 当前引导信息
const currentGuide = computed(() => props.guides[currentStep.value]);

// 高亮区域样式
const highlightStyle = ref({
  top: "0px",
  left: "0px",
  width: "0px",
  height: "0px",
});

// 提示框样式
const tooltipStyle = ref({
  top: "0px",
  left: "0px",
});

// 下一步
const nextStep = () => {
  if (currentStep.value < props.guides.length - 1) {
    currentStep.value++;
    updatePosition();
  } else {
    finishGuide();
  }
};

// 上一步
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--;
    updatePosition();
  }
};

// 完成引导
const finishGuide = () => {
  emit("finish");
};

// 更新高亮和提示位置
const updatePosition = () => {
  setTimeout(() => {
    const element = document.querySelector(currentGuide.value.element);
    if (element) {
      const rect = element.getBoundingClientRect();
      console.log({ element, rect });

      // 更新高亮区域
      highlightStyle.value = {
        top: `${rect.top - 5}px`,
        left: `${rect.left - 5}px`,
        width: `${rect.width + 10}px`,
        height: `${rect.height + 10}px`,
      };

      // 更新提示框位置
      const tooltip = {
        top: "auto",
        left: "auto",
        right: "auto",
        bottom: "auto",
      };

      const position = currentGuide.value.position || "bottom";
      const offset = 10;

      if (position === "bottom") {
        tooltip.top = `${rect.bottom + offset}px`;
        tooltip.left = "0px";
      } else if (position === "top") {
        tooltip.bottom = `${window.innerHeight - rect.top + offset}px`;
        tooltip.left = "0px";
      } else if (position === "left") {
        tooltip.top = `${rect.top}px`;
        tooltip.right = `${window.innerWidth - rect.left + offset}px`;
      } else if (position === "right") {
        tooltip.top = `${rect.top}px`;
        tooltip.left = `${rect.right + offset}px`;
      }

      tooltipStyle.value = tooltip;
    }
  }, 10);
};

// 监听窗口变化
onMounted(() => {
  updatePosition();
  window.addEventListener("resize", updatePosition);
});
</script>

<style lang="scss" scoped>
.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
}
.guide-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  //   background: #000000cc;
}
.highlight-box {
  position: absolute;
  border-radius: 8px;
  z-index: 2001;
  box-shadow: 0 0 0 2000px #000000cc;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.tooltip-container {
  position: absolute;
  width: 100%;
  overflow: hidden;
  z-index: 2002;
  transition: opacity 0.3s, transform 0.3s;
}

.tooltip-image {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tooltip-footer {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 48px;
  z-index: 2003;
  .step-btn {
    display: inline-flex;
    height: 48px;
    padding: 6px 32px;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    border-radius: 8px;
    border: 1px solid var(-----color-border-focused, #e1e6eb);
    background: var(-----color-other-1, rgba(255, 255, 255, 0.08));
    color: var(-----color-text-1, #f9fbfc);
    text-align: center;
    /* 正文/细体/font-size-14-400 */
    font-family: "PingFang SC";
    font-size: 14px;
    font-weight: 400;
    line-height: 24px /* 171.429% */;
  }
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.theme-selector {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin: 20px 0 30px;
}
</style>
