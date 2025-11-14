# SecondBrain Chrome Extension

SecondBrain 서비스를 위한 Chrome 확장프로그램

웹페이지를 저장하고 노트를 생성하여 개인 지식 베이스를 구축하는 Chrome Extension입니다.

## 기술 스택

### Core

- **Framework**: React 18.3.1
- **Language**: TypeScript 5.9.3
- **Build Tool**: Vite 7.1.12 + @crxjs/vite-plugin 2.2.1
- **Package Manager**: pnpm 10.21.0

### UI & Styling

- **UI Components**: Shadcn UI (New York style)
- **CSS Framework**: Tailwind CSS 4.1.17
- **Styling**: @tailwindcss/vite plugin
- **Icons**: Lucide React 0.553.0
- **Animations**: tw-animate-css 1.4.0

### Chrome Extension

- **Manifest**: Version 3
- **Polyfill**: webextension-polyfill 0.12.0
- **Browser API**: @types/chrome 0.1.27

### Code Quality

- **Linter**: ESLint 9.x (Flat Config)
- **Formatter**: Prettier 3.6.2
- **Git Hooks**: Husky 9.1.7 + lint-staged
- **Commit Lint**: @commitlint/cli 20.1.0

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

## 프로덕션 빌드

### 빌드 실행

```bash
pnpm build
```

빌드 결과물은 `dist/` 폴더에 생성됩니다.

## 프로젝트 구조

```markdown
extension/
├── src/
│ ├── background/ # Background Service Worker
│ │ └── service-worker.ts # 인증, 탭 관리, 메시지 통신
│ ├── content-scripts/ # Content Scripts
│ │ ├── overlay/ # 오버레이 UI 컴포넌트
│ │ │ ├── index.tsx # 엔트리 포인트
│ │ │ ├── OverlayRoot.tsx
│ │ │ ├── ExtensionOverlay.tsx
│ │ │ ├── LoginPrompt.tsx
│ │ │ ├── GoogleLoginButton.tsx
│ │ │ └── ActionButtons.tsx
│ │ └── overlay.css # Tailwind CSS v4 스타일
│ ├── components/ # UI 컴포넌트
│ │ └── ui/ # Shadcn UI 컴포넌트
│ │ └── button.tsx
│ ├── hooks/ # 커스텀 훅
│ │ └── useExtensionAuth.ts
│ ├── lib/ # 유틸리티 함수
│ │ └── utils.ts # cn, 헬퍼 함수
│ ├── config/ # 환경 설정
│ │ └── env.ts # 타입 안전 환경 변수
│ ├── popup/ # Popup UI (향후 확장용)
│ ├── index.css # Tailwind CSS v4 메인 스타일
│ ├── manifest.json # Extension Manifest V3
│ └── vite-env.d.ts # Vite 타입 정의
├── public/
│ └── assets/ # 정적 자산
│ ├── icon.png # 128x128
│ ├── icon-48.png
│ └── icon-16.png
├── vite.config.ts # Vite + Tailwind + CRXJS 설정
├── tsconfig.json # TypeScript 루트 설정
├── tsconfig.app.json # App TypeScript 설정
├── tsconfig.node.json # Node TypeScript 설정
├── eslint.config.js # ESLint Flat Config
├── .prettierrc.js # Prettier 설정
├── .editorconfig # 에디터 설정
├── .env.example # 환경 변수 예시
├── components.json # Shadcn UI 설정
└── package.json
```

## 주요 기능

### 1. 웹페이지 콘텐츠 캡처

- 모든 웹페이지에서 확장 프로그램 아이콘 클릭
- 오버레이 UI를 통해 페이지 저장
- Shadow DOM으로 스타일 격리

### 2. 인증 시스템

- Google OAuth 2.0 인증
- JWT 토큰 기반 세션 관리
- chrome.storage.local에 안전한 토큰 저장
- 자동 로그인 상태 동기화

### 3. Background Service Worker

- OAuth 인증 플로우 처리
- Content Scripts와 메시지 통신
- 탭 생성/관리 (OAuth 팝업)
- 동적 Content Script 주입

### 4. UI 컴포넌트

- Shadcn UI 컴포넌트 시스템
- Tailwind CSS v4 스타일링 (자동 최적화)
- 다크 모드 지원 (OKLCH 색상)
- 반응형 디자인

## 스크립트

### 개발

```bash
pnpm dev          # 개발 서버 시작 (HMR 지원)
pnpm preview      # 빌드 미리보기
```

### 품질 관리

```bash
pnpm lint         # ESLint 검사
pnpm lint:fix     # ESLint 자동 수정
pnpm format       # Prettier 포맷팅
pnpm format:check # Prettier 검사
pnpm typecheck    # TypeScript 타입 체크
```

### 빌드

```bash
pnpm build        # 프로덕션 빌드
```

## 배포

### Chrome Web Store 배포

1. **프로덕션 빌드**

   ```bash
   pnpm build
   ```

2. **dist/ 폴더 압축**

   ```bash
   cd dist
   zip -r ../extension.zip .
   ```

3. **Chrome Web Store 업로드**
   - [Chrome Web Store Developer Dashboard](https://chrome.google.com/webstore/devconsole) 접속
   - 새 항목 생성 또는 기존 항목 업데이트
   - `extension.zip` 업로드
   - 스토어 등록 정보 입력
   - 검토 제출

## 권한 설명

현재 확장 프로그램이 요청하는 권한:

- **storage**: 인증 토큰 및 사용자 정보 저장
- **tabs**: OAuth 탭 생성/관리, 메시지 전송
- **activeTab**: 현재 활성 탭 접근
- **scripting**: Content Script 동적 주입
- **host_permissions**: API 서버 통신 (<https://api.brainsecond.site>)

## 기술 특징

### Tailwind CSS v4

- **Zero Configuration**: content 설정 불필요
- **자동 최적화**: 사용되는 클래스만 포함 (~10KB)
- **Lightning CSS**: Rust 기반 초고속 빌드
- **OKLCH 색상**: 더 정확한 색상 표현

### TypeScript 경로 별칭

```typescript
@/           → src/
@components/ → src/components/
@hooks/      → src/hooks/
@lib/        → src/lib/
@config/     → src/config/
```

### 빌드 최적화

- Tree-shaking 자동 적용
- Code splitting (필요시)
- Sourcemap (개발 환경만)
- 크기 경고: 1MB 이상 청크

## 문제 해결

### Extension이 로드되지 않는 경우

1. `pnpm build`로 재빌드
2. Chrome에서 확장 프로그램 새로고침
3. 콘솔 에러 확인

### OAuth 인증 실패

1. `.env` 파일 확인
2. API 서버 연결 확인
3. host_permissions 확인

### Content Script 주입 실패

1. 페이지 새로고침
2. 확장 프로그램 아이콘 다시 클릭
3. Background Service Worker 콘솔 확인
