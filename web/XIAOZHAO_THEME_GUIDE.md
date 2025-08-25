# 校招活动主题配置说明

## 功能概述

根据 `route.query.activity` 的值来区分不同的活动主题。当 `activity=xiaozhao` 时，页面会应用校招活动的特殊样式和配置。

## 校招活动主题变更

### 1. 语言切换
- **默认主题**: 显示语言切换按钮，支持中英文切换
- **校招主题**: 隐藏语言切换按钮，只显示中文

### 2. Logo 显示
- **默认主题**: 根据语言显示对应的 logo
- **校招主题**: 
  - 在中文 logo 前面增加校招专用 logo
  - 两个 logo 之间间距 12px
  - 高度保持一致 (32px)
  - 校招 logo 地址: `https://tx.a.yximgs.com/kos/nlav12119/ffBJkwTW_2025-08-25-22-34-08.png`

### 3. Banner 图片
- **默认主题**: 根据语言和类型显示对应的 banner
- **校招主题**: 使用固定的校招 banner
  - 图片地址: `https://ali.a.yximgs.com/kos/nlav12119/LYXfjgtf_2025-08-25-22-36-33.png`

### 4. 容器背景
- **默认主题**: 使用默认背景图
- **校招主题**: 使用校招专用背景图
  - 背景图地址: `https://tx.a.yximgs.com/kos/nlav12119/SNqERzGo_2025-08-25-22-38-23.png`

### 5. 生成按钮样式
- **默认主题**: 
  ```css
  background: linear-gradient(98.88deg, #f7ffe0 0.35%, #74ff52 50.35%, #1bf6fd 100.35%);
  ```
- **校招主题**: 
  ```css
  background: linear-gradient(99deg, #FF4906 0.35%, #FEA623 100.35%);
  color: #FFF;
  ```

### 6. 提示文字颜色
- **默认主题**: 使用默认颜色 `#5E6266ff`
- **校招主题**: 使用 `var(--color-text-5)`

## 使用方法

### 访问校招主题页面
在 URL 中添加 `activity=xiaozhao` 参数：
```
/creation/image?activity=xiaozhao
/creation/video?activity=xiaozhao
```

### 访问默认主题页面
不添加 `activity` 参数或使用其他值：
```
/creation/image
/creation/video
/creation/image?activity=default
```

## 技术实现

### 1. 主题检测
```typescript
const isXiaozhaoActivity = computed(() => route.query.activity === 'xiaozhao');
```

### 2. 资源配置
```typescript
const assets = computed(() => {
  const isZh = locale.value === "zh-CN";
  
  if (isXiaozhaoActivity.value) {
    // 校招活动主题配置
    return {
      logo: logoZh, // 只使用中文logo
      xiaozhaoLogo: 'https://tx.a.yximgs.com/kos/nlav12119/ffBJkwTW_2025-08-25-22-34-08.png',
      banner: 'https://ali.a.yximgs.com/kos/nlav12119/LYXfjgtf_2025-08-25-22-36-33.png',
      // ...
    };
  } else {
    // 默认主题配置
    // ...
  }
});
```

### 3. 条件渲染
```vue
<!-- 校招logo显示 -->
<img
  v-if="isXiaozhaoActivity && assets.xiaozhaoLogo"
  class="h-32px"
  :src="assets.xiaozhaoLogo"
  alt="校招logo"
/>

<!-- 语言切换按钮 -->
<LangSwitcher
  v-if="!isXiaozhaoActivity"
  class="right-20px top-0 absolute"
  @change="onLocaleChange"
/>
```

### 4. 样式应用
```vue
<div
  class="creation-container"
  :class="{ 'xiaozhao-theme': isXiaozhaoActivity }"
>
```

```less
.creation-container {
  &.xiaozhao-theme {
    background-image: url(https://tx.a.yximgs.com/kos/nlav12119/SNqERzGo_2025-08-25-22-38-23.png);
    
    .generate-btn {
      background: linear-gradient(99deg, #FF4906 0.35%, #FEA623 100.35%);
      .van-button__content .van-button__text {
        color: #FFF;
      }
    }
    
    .warning-tip {
      color: var(--color-text-5);
    }
  }
}
```

## 测试验证

1. **默认主题测试**:
   - 访问 `/creation/image`
   - 确认显示语言切换按钮
   - 确认使用默认背景和按钮样式

2. **校招主题测试**:
   - 访问 `/creation/image?activity=xiaozhao`
   - 确认隐藏语言切换按钮
   - 确认显示校招logo和banner
   - 确认使用校招背景和按钮样式

## 注意事项

1. 校招活动主题强制使用中文，不支持语言切换
2. 所有图片资源都使用了 CDN 地址，确保加载速度
3. 样式使用了 CSS 变量 `--color-text-5`，需要确保在项目中已定义
4. 主题切换是响应式的，URL 参数变化时会自动更新