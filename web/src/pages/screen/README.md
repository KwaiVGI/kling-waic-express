# 图片展示屏幕组件

## 功能说明

这个组件实现了双层设计的图片展示功能，支持以下特性：

### 1. 双层设计架构

- **图片容器**：宽高比可通过接口配置（默认9:16），长边适配屏幕尺寸
- **图片本身**：固定2:3比例，宽度与容器等宽，位置在容器最下面

### 2. 容器宽高比适配

- 默认按照 9:16 的宽高比创建容器
- 长边会自动适配屏幕尺寸（宽屏时以宽度为准，竖屏时以高度为准）
- 支持通过接口动态获取容器宽高比配置

### 3. 支持的容器宽高比

- 9:16 (竖屏容器，默认)
- 16:9 (横屏容器)
- 4:3 (传统容器)
- 3:4 (竖屏容器)
- 1:1 (正方形容器)
- 21:9 (超宽屏容器)
- 自定义比例

### 4. 图片定位规则

- 图片固定为 2:3 宽高比
- 宽度始终与容器等宽
- 垂直位置固定在容器最下面
- 使用 `background-size: cover` 确保图片填充

### 5. 测试方法

#### 通过URL参数测试不同容器宽高比：

```
# 9:16 竖屏容器
http://localhost:3000/screen/image?containerRatio=9:16

# 16:9 横屏容器
http://localhost:3000/screen/image?containerRatio=16:9

# 4:3 传统容器
http://localhost:3000/screen/image?containerRatio=4:3
```

### 6. API接口

#### 获取容器宽高比

```typescript
// 接口路径（待实现）
GET /api/aspect-ratio

// 返回格式
{
  "ratio": [9, 16],  // 容器宽高比
  "success": true
}
```

#### 设置容器宽高比（可选）

```typescript
// 接口路径（待实现）
POST /api/aspect-ratio

// 请求格式
{
  "ratio": [16, 9]  // 容器宽高比
}
```

### 7. 代码结构

```
web/src/pages/screen/
├── image.vue                 # 主组件
├── README.md                # 说明文档
└── ...

web/src/api/
├── aspectRatioService.ts    # 容器宽高比服务
└── ...
```

### 8. 实现原理

1. **双层架构**：
   - 外层容器：动态宽高比，适配屏幕
   - 内层图片：固定2:3比例，底部对齐

2. **动态计算容器尺寸**：根据容器宽高比和屏幕尺寸计算最适合的容器大小

3. **图片定位算法**：

   ```typescript
   // 使用 padding-bottom 技巧保持宽高比
   const paddingBottomPercent = (imgHeight / imgWidth) * 100 // 3/2 * 100 = 150%

   return {
     width: '100%', // 与容器等宽
     height: '0', // 高度设为0
     paddingBottom: `${paddingBottomPercent}%`, // 150%，相对于自身宽度
     bottom: '0', // 底部对齐
   }
   ```

4. **响应式设计**：监听窗口大小变化，自动重新计算

5. **宽高比保持技巧**：
   - 使用 `padding-bottom` 而不是 `height` 来保持图片宽高比
   - `padding-bottom: 150%` 表示高度为宽度的150%（即2:3比例）
   - 这是因为 `padding` 的百分比值是相对于元素自身宽度计算的
   - `height: 0` 确保元素高度完全由 `padding-bottom` 决定

6. **Mock服务**：提供完整的Mock实现，支持URL参数测试

### 9. 下一步工作

- [ ] 实现真实的API接口
- [ ] 添加容器宽高比切换动画
- [ ] 支持图片宽高比也可配置
- [ ] 添加容器宽高比配置管理界面
- [ ] 优化图片加载和缓存策略
