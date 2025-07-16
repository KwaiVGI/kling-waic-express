export function useGuide() {
      const showGuide = ref(false);
      const currentGuides = ref([]);
      const currentTheme = ref({});
      const guideRef = ref(null);
      
      // 默认引导配置
      const defaultGuides = [
       
      ];
      
      // 自定义主题
      const themes = {
        default: {},
        orange: {
          headerStart: '#ff976a',
          headerEnd: '#ff6034',
          highlightColor: '#ff6034',
          highlightGlow: 'rgba(255, 96, 52, 0.7)'
        },
        purple: {
          headerStart: '#7232dd',
          headerEnd: '#c644fc',
          highlightColor: '#c644fc',
          highlightGlow: 'rgba(198, 68, 252, 0.7)'
        },
        teal: {
          headerStart: '#00bcd4',
          headerEnd: '#00c853',
          highlightColor: '#00c853',
          highlightGlow: 'rgba(0, 200, 83, 0.7)'
        }
      };
      
      // 开始引导
      const startGuide = (guides, theme = 'default') => {
        currentGuides.value = guides || defaultGuides;
        currentTheme.value = themes[theme] || themes.default;
        showGuide.value = true;
      };
      
      // 开始单步引导
      const startStepGuide = (stepConfig, theme = 'default') => {
        currentGuides.value = [stepConfig];
        currentTheme.value = themes[theme] || themes.default;
        showGuide.value = true;
      };
      
      // 完成引导
      const finishGuide = () => {
        showGuide.value = false;
      };
      
      return {
        showGuide,
        currentGuides,
        currentTheme,
        guideRef,
        startGuide,
        startStepGuide,
        finishGuide,
        themes
      };
    }