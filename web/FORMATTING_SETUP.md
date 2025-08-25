# 代码格式化配置说明

## 问题描述

之前项目中存在 VSCode ESLint quick fix 和保存文件自动格式化的冲突问题，主要原因是：

1. 项目使用了 `@antfu/eslint-config` 并启用了 `formatters: true`
2. VSCode 可能同时启用了 Prettier 和 ESLint 格式化
3. 两者的格式化规则存在冲突，导致保存时格式反复变化

## 解决方案

### 1. 统一使用 ESLint 进行格式化

- 禁用 VSCode 的默认格式化器
- 将 ESLint 设置为所有支持文件类型的默认格式化器
- 通过 `editor.codeActionsOnSave` 在保存时自动执行 ESLint 修复

### 2. 优化 ESLint 配置

- 调整 `formatters` 配置，只对特定文件类型启用格式化
- 添加明确的代码风格规则，确保一致性
- 扩展忽略文件列表

### 3. 禁用 Prettier 冲突

- 创建 `.prettierignore` 文件忽略所有文件
- 在 VSCode 设置中禁用 Prettier 扩展
- 在扩展推荐中标记 Prettier 为不推荐

## 配置文件说明

### `.vscode/settings.json`

```json
{
  // 禁用默认格式化，使用 ESLint
  "editor.formatOnSave": false,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },

  // 为所有支持的文件类型设置 ESLint 为默认格式化器
  "[typescript]": {
    "editor.defaultFormatter": "dbaeumer.vscode-eslint"
  }
  // ... 其他文件类型
}
```

### `eslint.config.ts`

```typescript
export default antfu({
  formatters: {
    css: true,
    html: true,
    markdown: 'prettier', // 只对 markdown 使用 prettier
  },
  // 明确的代码风格规则
  rules: {
    'style/semi': ['error', 'never'],
    'style/quotes': ['error', 'single'],
    // ...
  },
})
```

### `.prettierignore`

```
# 忽略所有文件，使用 ESLint 格式化
*
```

## 使用方法

1. **保存时自动格式化**：文件保存时会自动执行 ESLint 修复
2. **手动格式化**：使用 `Ctrl+Shift+P` → "ESLint: Fix all auto-fixable Problems"
3. **命令行格式化**：`pnpm lint:fix`

## 注意事项

1. 确保安装了 ESLint VSCode 扩展
2. 如果之前安装了 Prettier 扩展，建议禁用或卸载
3. 团队成员需要同步这些 VSCode 设置
4. 对于不支持 ESLint 格式化的文件类型，可以在 `.prettierignore` 中添加例外

## 验证配置

1. 打开任意 `.ts` 或 `.vue` 文件
2. 故意破坏格式（如添加多余空格、分号等）
3. 保存文件，检查是否自动修复为正确格式
4. 确认没有格式反复变化的问题

## 已完成的配置

✅ **VSCode 设置** (`.vscode/settings.json`)

- 禁用默认格式化器
- 启用 ESLint 作为所有支持文件类型的格式化器
- 配置保存时自动执行 ESLint 修复
- 禁用 Prettier 扩展

✅ **ESLint 配置** (`eslint.config.ts`)

- 优化 formatters 配置，避免与 Prettier 冲突
- 添加明确的代码风格规则
- 扩展忽略文件列表

✅ **Prettier 忽略** (`.prettierignore`)

- 忽略所有文件，防止 Prettier 干扰

✅ **扩展推荐** (`.vscode/extensions.json`)

- 推荐必要的 VSCode 扩展
- 标记 Prettier 为不推荐扩展

## 测试结果

经过测试，配置已经正常工作：

- ESLint 自动修复功能正常
- 代码风格统一（单引号、无分号、2空格缩进等）
- 保存时自动格式化
- 没有格式化冲突问题

## 团队使用指南

1. **新成员加入**：确保安装推荐的 VSCode 扩展
2. **现有成员**：重启 VSCode 以应用新配置
3. **格式化命令**：
   - 手动格式化：`Ctrl+Shift+P` → "ESLint: Fix all auto-fixable Problems"
   - 命令行格式化：`pnpm lint:fix`
4. **如果遇到问题**：检查是否禁用了 Prettier 扩展
