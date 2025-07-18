export function useGuide() {
  const showGuide = ref(false);
  const currentGuides = ref([]);
  const guideRef = ref(null);

  // 默认引导配置
  const defaultGuides = [];

  // 开始引导
  const startGuide = (guides) => {
    currentGuides.value = guides || defaultGuides;
    showGuide.value = true;
  };

  // 开始单步引导
  const startStepGuide = (stepConfig) => {
    currentGuides.value = [stepConfig];
    showGuide.value = true;
  };

  // 完成引导
  const finishGuide = () => {
    showGuide.value = false;
  };

  return {
    showGuide,
    currentGuides,
    guideRef,
    startGuide,
    startStepGuide,
    finishGuide,
  };
}
