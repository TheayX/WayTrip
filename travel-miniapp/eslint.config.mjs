export default [
  {
    files: ['src/**/*.js'],
    ignores: ['node_modules/**', 'dist/**', 'unpackage/**'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        uni: 'readonly',
        wx: 'readonly',
        getCurrentPages: 'readonly',
        console: 'readonly'
      }
    },
    rules: {
      'no-undef': 'error',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]
    }
  }
]

