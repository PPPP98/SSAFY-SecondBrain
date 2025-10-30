import js from '@eslint/js';
import globals from 'globals';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import tailwindcss from 'eslint-plugin-tailwindcss';
import noRelativeImportPaths from 'eslint-plugin-no-relative-import-paths';
import eslintConfigPrettier from 'eslint-config-prettier';
import { defineConfig, globalIgnores } from 'eslint/config';

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommendedTypeChecked,
      react.configs.flat.recommended,
      react.configs.flat['jsx-runtime'],
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite,
      ...tailwindcss.configs['flat/recommended'],
    ],
    languageOptions: {
      ecmaVersion: 'latest',
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'no-relative-import-paths': noRelativeImportPaths,
    },
    settings: {
      react: {
        version: 'detect',
      },
      tailwindcss: {
        callees: ['cn', 'cva'],
        config: 'tailwind.config.js',
      },
    },
    rules: {
      // 절대 경로 강제 규칙
      'no-relative-import-paths/no-relative-import-paths': [
        'error',
        { allowSameFolder: true, rootDir: 'src', prefix: '@' },
      ],

      // TypeScript 네이밍 컨벤션
      '@typescript-eslint/naming-convention': [
        'error',
        // 변수: camelCase, PascalCase, UPPER_CASE 허용
        {
          selector: 'variable',
          format: ['camelCase', 'PascalCase', 'UPPER_CASE'],
          leadingUnderscore: 'allow',
        },
        // 함수: camelCase, PascalCase 허용
        {
          selector: 'function',
          format: ['camelCase', 'PascalCase'],
        },
        // 타입 관련: PascalCase만 허용
        {
          selector: 'typeLike',
          format: ['PascalCase'],
        },
        // 인터페이스: "I" 접두사 금지 (Hungarian notation 방지)
        {
          selector: 'interface',
          format: ['PascalCase'],
          custom: {
            regex: '^I[A-Z]',
            match: false,
          },
        },
        // 타입 별칭: "T" 접두사 금지
        {
          selector: 'typeAlias',
          format: ['PascalCase'],
          custom: {
            regex: '^T[A-Z]',
            match: false,
          },
        },
        // 타입 파라미터: "T" 단독 사용 금지
        {
          selector: 'typeParameter',
          format: ['PascalCase'],
          custom: {
            regex: '^T$|^T[A-Z]',
            match: false,
          },
        },
      ],
    },
  },
  // Prettier와 충돌하는 ESLint 규칙 비활성화 (반드시 마지막에 위치)
  eslintConfigPrettier,
]);
