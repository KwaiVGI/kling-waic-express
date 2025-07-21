import { ref, onMounted, onBeforeUnmount, watch, type Ref } from "vue";

interface UseZoomOptions {
  topOffset?: number;
  bottomOffset?: number;
}

export function useZoom(
  contentRef: Ref<HTMLElement | null>,
  containerRef: Ref<HTMLElement | null> = ref(null),
  options: UseZoomOptions = {}
) {
  //   const { topOffset = 0, bottomOffset = 200 } = options;
  const originalHeight = ref(0);
  const originalWidth = ref(0);
  const scaleRatio = ref(1);

  // 获取可用高度（容器高度或视口高度）
  const getUsableHeight = () => {
    // if (containerRef.value) {
    //   return containerRef.value.clientHeight;
    // }
    return window.innerHeight;
  };

  // 更新内容原始尺寸
  const updateOriginalDimensions = () => {
    if (!contentRef.value) return;

    // const prevTransform = contentRef.value.style.transform;
    // const prevWidth = contentRef.value.style.width;
    // const prevHeight = contentRef.value.style.height;

    // // 临时移除样式以获取原始尺寸
    // contentRef.value.style.transform = "";
    // contentRef.value.style.width = "";
    // contentRef.value.style.height = "";

    // // 获取原始尺寸
    // originalHeight.value = contentRef.value.scrollHeight;
    // originalWidth.value = contentRef.value.scrollWidth;

    // // 恢复样式
    // contentRef.value.style.transform = prevTransform;
    // contentRef.value.style.width = prevWidth;
    // contentRef.value.style.height = prevHeight;
  };

  // 计算缩放比例
  const calculateScaleRatio = () => {
    if (originalHeight.value === 0) return;

    const usableHeight = getUsableHeight();
    let ratio = Math.min(1, usableHeight / originalHeight.value);
    ratio = Math.max(0.5, ratio);

    scaleRatio.value = ratio;
  };

  // 处理尺寸变化
  const handleResize = () => {
    updateOriginalDimensions();
    calculateScaleRatio();
  };

  onMounted(() => {
    if (!contentRef.value) return;

    // 初始化尺寸
    updateOriginalDimensions();
    calculateScaleRatio();

    // 监听窗口和容器变化
    window.addEventListener("resize", handleResize);

    // 使用 ResizeObserver 监听内容变化
    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(contentRef.value);

    // 如果使用容器，监听容器变化
    if (containerRef.value) {
      resizeObserver.observe(containerRef.value);
    }

    // 清理操作
    onBeforeUnmount(() => {
      // window.removeEventListener('resize', handleResize);
      resizeObserver.disconnect();
    });
  });

  // 当内容引用变化时重新初始化
  watch(contentRef, (newEl) => {
    if (newEl) {
      updateOriginalDimensions();
      calculateScaleRatio();
    }
  });

  return scaleRatio;
}
