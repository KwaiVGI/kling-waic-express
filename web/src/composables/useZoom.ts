import { ref, onMounted, onBeforeUnmount, watch, type Ref } from "vue";

export function useZoom(contentRef: Ref<HTMLElement | null>) {
  const scaleRatio = ref(1);
  // 使用防抖避免频繁调整
  let resizeTimer: ReturnType<typeof setTimeout> | null = null;

  // 获取可用高度（容器高度或视口高度）
  const getUsableHeight = () => {
    // 优先使用 visualViewport 获取准确高度
    return window.visualViewport?.height || window.innerHeight;
  };
  function isIphone() {
    const ua = navigator.userAgent;
    return /iPhone/.test(ua) && !/iPad/.test(ua);
  }

  // 计算缩放比例
  const updateScale = () => {
    // 苹果手机有些bug 暂不缩放
    if (isIphone()) {
      return 1;
    }
    if (!contentRef.value) return;
    const contentHeight = contentRef.value.scrollHeight;
    if (contentHeight === 0) return;
    const usableHeight = getUsableHeight();
    let ratio = Math.min(1, usableHeight / contentHeight);
    ratio = Math.max(0.5, ratio);
    // 仅当比例变化超过阈值时更新
    if (Math.abs(ratio - scaleRatio.value) > 0.01) {
      scaleRatio.value = ratio;
    }
  };

  // 处理尺寸变化
  const handleResize = () => {
    // 使用防抖避免频繁调整
    if (resizeTimer) clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      updateScale();
      resizeTimer = null;
    }, 100);
  };

  const resizeObserver = new ResizeObserver(handleResize);
  onMounted(() => {
    if (!contentRef.value) return;

    // 初始更新
    updateScale();

    // 监听窗口和容器变化
    window.addEventListener("resize", handleResize);
    window.visualViewport?.addEventListener("resize", handleResize);

    // 使用 ResizeObserver 监听内容变化
    resizeObserver.observe(contentRef.value);
  });
  // 清理操作
  onBeforeUnmount(() => {
    window.removeEventListener("resize", handleResize);
    window.visualViewport?.removeEventListener("resize", handleResize);
    resizeObserver.disconnect();
  });

  // 当内容引用变化时重新初始化
  watch(contentRef, (newEl) => {
    if (newEl) {
      updateScale();
    }
  });

  return scaleRatio;
}
