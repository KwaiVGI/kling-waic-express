import antfu from '@antfu/eslint-config'

export default antfu(
  {
    vue: true,
    typescript: true,

    // Enable UnoCSS support
    // https://unocss.dev/integrations/vscode
    unocss: true,
    formatters: {
      css: false,
      html: false,
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
      'style/brace-style': ['error', '1tbs', { allowSingleLine: true }],
      'style/object-curly-spacing': ['error', 'always'],
      'style/array-bracket-spacing': ['error', 'never'],
      'style/space-before-function-paren': ['error', 'never'],
      'style/keyword-spacing': ['error', { before: true, after: true }],
      'style/space-infix-ops': 'error',
      'style/no-trailing-spaces': 'error',
      'style/eol-last': ['error', 'always'],
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
