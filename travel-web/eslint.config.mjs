export default [
  {
    files: ['src/**/*.js'],
    ignores: ['node_modules/**', 'dist/**'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        window: 'readonly',
        document: 'readonly',
        navigator: 'readonly',
        FormData: 'readonly',
        URL: 'readonly',
        Blob: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        console: 'readonly'
      }
    },
    rules: {
      'no-undef': 'error',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-restricted-imports': ['error', {
        paths: [
          {
            name: '@/components/AiChatWidget.vue',
            message: 'AiChatWidget 已迁移到 shared 层，请使用 @/shared/ui/AiChatWidget.vue'
          },
          {
            name: '@/api/ai.js',
            message: 'AI API 已迁移到 shared 层，请使用 @/shared/api/ai.js'
          }
        ],
        patterns: [
          {
            group: ['@/layout/*'],
            message: '布局模块已迁移到 app/layouts，请使用 @/app/layouts/*'
          }
        ]
      }]
    }
  }
]

