import js from '@eslint/js'
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript'
import importX from 'eslint-plugin-import-x'
import reactHooks from 'eslint-plugin-react-hooks'
import globals from 'globals'
import tseslint from 'typescript-eslint'

const expectCall =
  ":matches(CallExpression[callee.name='expect'], CallExpression[callee.object.name='expect'][callee.property.name=/^(soft|poll)$/])"
const notATestingLibraryQuery =
  ":not([callee.object.name='screen']):not([callee.object.callee.name='within']):not([callee.name='within'])"
const actInAssertionMessage =
  'Name the act as a value before expect(…): a call never sits inside the assertion, only Testing Library queries (screen.…, within(…).…) may.'

export default tseslint.config(
  {
    ignores: ['build/**', '.gradle/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.strict,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { 'import-x': importX, 'react-hooks': reactHooks },
    settings: {
      'import-x/resolver-next': [createTypeScriptImportResolver()],
    },
    rules: {
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'error',
      'import-x/no-restricted-paths': [
        'error',
        {
          zones: [
            {
              target: './src/domain',
              from: ['./src/api', './src/ui', './src/app', './src/login'],
            },
            {
              target: './src/api',
              from: ['./src/ui', './src/app', './src/login'],
            },
            {
              target: './src/ui',
              from: ['./src/api', './src/app', './src/login'],
            },
            { target: './src/app', from: './src/login' },
            { target: './src/login', from: './src/app' },
          ],
        },
      ],
    },
  },
  {
    files: ['src/**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: `${expectCall} :matches(CallExpression${notATestingLibraryQuery}, NewExpression, TaggedTemplateExpression, ImportExpression)`,
          message: actInAssertionMessage,
        },
        {
          selector: `${expectCall} > :matches(ArrowFunctionExpression, FunctionExpression)`,
          message: actInAssertionMessage,
        },
      ],
    },
  },
)
