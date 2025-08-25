import antfu from '@antfu/eslint-config'

export default antfu(
  {
    vue: true,
    typescript: true,

    // Enable UnoCSS support
    // https://unocss.dev/integrations/vscode
    unocss: true,
    formatters: {
      css: true,
      html: true,
      markdown: 'prettier',
    },
  },
  {
    rules: {
      'perfectionist/sort-imports': 'off',
      'perfectionist/sort-exports': 'off',
      'perfectionist/sort-named-exports': 'off',

      // 确保格式化规则一致
      'style/semi': ['error', 'never'],
      'style/quotes': ['error', 'single'],
      'style/indent': ['error', 2],
      'style/comma-dangle': ['error', 'always-multiline'],
    },
  },
  {
    ignores: [
      '.github/**',
      'dist/**',
      'node_modules/**',
      '*.min.*',
    ],
  },
)
