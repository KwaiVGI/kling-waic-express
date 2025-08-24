// 此文件不支持热更新，修改后需要重启生效

// 需要转换的 fixed 选择器列表
const rootContainingBlockSelectorList = [
  ".van-tabbar",
  ".van-popup",
  ".van-popup--bottom",
  ".van-popup--top",
  ".van-popup--left",
  ".van-popup--right",
  // 在这里添加你的选择器
];

export default {
  plugins: {
    autoprefixer: {
      overrideBrowserslist: ["defaults", "safari >= 9", "ios_saf >= 9"],
    },

    // https://github.com/wswmsword/postcss-mobile-forever
    "postcss-mobile-forever": {
      appSelector: "#h5App",
      viewportWidth: 414,
      maxDisplayWidth: 600,
      rootContainingBlockSelectorList,
      selectorBlackList: [
        ".pc-modal-mask",
        ".display-screen",
        ".ds-image-container",
        ".ds-casting-image",
        ".ds-container",
        ".ds-password-container",
        ".ds-modal-mask",
        ".viewer-modal",
        ".video-viewer-modal",
        ".logout-container",
      ],
    },
  },
};
