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
        getComputedStyle: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        console: 'readonly'
      }
    },
    rules: {
      'no-undef': 'error',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]
    }
  }
]

