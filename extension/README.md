# SecondBrain Chrome Extension

SecondBrain 서비스를 위한 Chrome 확장프로그램

## 기술 스택

- **Framework**: React 18.3.1
- **Language**: TypeScript 5.9.3
- **Build Tool**: Vite 7.1.12 + @crxjs/vite-plugin
- **Package Manager**: pnpm

## 개발 환경 설정

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

### 2. 의존성 설치

```bash
pnpm install
```

### 3. 개발 서버 실행

```bash
pnpm dev
```

개발 서버가 `http://localhost:5174`에서 실행됩니다.

### 4. 크롬에 확장프로그램 로드

1. 크롬 브라우저에서 `chrome://extensions` 열기
2. 우측 상단 "개발자 모드" 활성화
3. "압축해제된 확장 프로그램을 로드합니다" 클릭
4. `dist` 폴더 선택

## 빌드

### 프로덕션 빌드

```bash
pnpm build
```

빌드 결과물은 `dist/` 폴더에 생성됩니다.

## 프로젝트 구조

```
extension/
├── src/
│   ├── background/          # Background Service Worker
│   ├── content-scripts/     # Content Scripts
│   ├── hooks/               # Custom Hooks
│   ├── config/              # 환경 설정
│   └── manifest.json        # Extension Manifest
├── public/
│   └── assets/              # 아이콘 등 정적 자산
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 주요 기능

- 웹페이지 저장 및 노트 생성
- Google OAuth 2.0 인증
- Shadow DOM을 통한 스타일 격리
- Background Service Worker 통신

## 배포

Chrome Web Store에 수동 업로드

1. `pnpm build`로 프로덕션 빌드
2. `dist/` 폴더를 zip으로 압축
3. Chrome Web Store Developer Dashboard에서 업로드
