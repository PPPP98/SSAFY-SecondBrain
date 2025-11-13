import js from '@eslint/js';
import globals from 'globals';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
// import tailwindcss from 'eslint-plugin-tailwindcss'; // Tailwind v4와 호환 문제로 비활성화
import noRelativeImportPaths from 'eslint-plugin-no-relative-import-paths';
import tseslint from 'typescript-eslint';
import eslintConfigPrettier from 'eslint-config-prettier';

export default tseslint.config(
  { ignores: ['dist'] },

  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommendedTypeChecked,
      react.configs.flat.recommended,
      react.configs.flat['jsx-runtime'],
      // ...tailwindcss.configs['flat/recommended'], // Tailwind v4와 호환 문제로 비활성화
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.browser,
        ...globals.webextensions,
        chrome: 'readonly',
      },
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      // tailwindcss, // Tailwind v4와 호환 문제로 비활성화
      'no-relative-import-paths': noRelativeImportPaths,
    },
    settings: {
      react: {
        version: 'detect',
      },
      // tailwindcss 설정은 prettier-plugin-tailwindcss가 처리
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // Tailwind 클래스 정렬은 prettier-plugin-tailwindcss가 처리
      'no-relative-import-paths/no-relative-import-paths': [
        'warn',
        { allowSameFolder: true, rootDir: 'src', prefix: '@' },
      ],
      // React Three Fiber: 커스텀 props 허용
      'react/no-unknown-property': [
        'error',
        {
          ignore: [
            // Three.js 메쉬 속성
            'geometry',
            'material',
            'position',
            'rotation',
            'scale',
            // 조명 속성
            'intensity',
            'color',
            'castShadow',
            'receiveShadow',
            // 재질 속성
            'metalness',
            'roughness',
            'flatShading',
            'wireframe',
            'opacity',
            'transparent',
            'side',
            'depthWrite',
            // 기타 Three.js 속성
            'args',
            'attach',
            'dispose',
          ],
        },
      ],
    },
  },
  eslintConfigPrettier,
);
