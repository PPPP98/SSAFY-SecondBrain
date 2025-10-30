export default {
  parserPreset: {
    parserOpts: {
      // 팀 규칙: [domain] type: subject 형식 파싱
      headerPattern:
        /^\[(FE|BE|AI|DB)\] (feat|fix|docs|style|refactor|test|build|ci|perf|chore|revert): (.+)$/,
      headerCorrespondence: ['scope', 'type', 'subject'],
    },
  },
  rules: {
    // Domain (scope) 검증
    'scope-enum': [2, 'always', ['FE', 'BE', 'AI', 'DB']],
    'scope-empty': [2, 'never'],
    'scope-case': [2, 'always', 'upper-case'],

    // Type 검증
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'test',
        'build',
        'ci',
        'perf',
        'chore',
        'revert',
      ],
    ],
    'type-empty': [2, 'never'],
    'type-case': [2, 'always', 'lower-case'],

    // Subject 검증
    'subject-empty': [2, 'never'],
    'subject-case': [0], // 한글 사용을 위해 비활성화
    'subject-full-stop': [2, 'never', '.'], // 마침표 금지

    // Header 길이 제한
    'header-max-length': [2, 'always', 100],

    // Body는 선택사항
    'body-leading-blank': [1, 'always'],
    'body-max-line-length': [0],
  },
};
