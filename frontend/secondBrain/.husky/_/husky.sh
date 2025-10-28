#!/usr/bin/env sh

# NVM 환경 로드 (macOS/Linux)
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  . "$NVM_DIR/nvm.sh"
  # .nvmrc 파일이 있으면 해당 Node.js 버전 사용
  if [ -f "$(git rev-parse --show-toplevel)/frontend/secondBrain/.nvmrc" ]; then
    nvm use 2>/dev/null || true
  fi
fi

# Windows용 NVM 로드 (Git Bash)
if [ -s "$NVM_DIR/nvm.sh" ]; then
  : # 위에서 이미 로드됨
elif [ -n "$NVM_HOME" ]; then
  # NVM for Windows는 NVM_HOME 환경 변수 사용
  export PATH="$NVM_HOME:$NVM_SYMLINK:$PATH"
fi

# 대체 경로: Node.js 설치 경로 추가 (크로스 플랫폼)
# node가 없을 경우에만 아래 경로들을 순서대로 확인
command -v node >/dev/null 2>&1 || {
  export PATH="$HOME/.nvm/versions/node/$(nvm current 2>/dev/null)/bin:$PATH"
  export PATH="/usr/local/bin:$PATH"                    # macOS Homebrew (Intel)
  export PATH="/opt/homebrew/bin:$PATH"                 # macOS Homebrew (M1/M2)
  export PATH="$HOME/.local/bin:$PATH"                  # Linux
  export PATH="$APPDATA/npm:$PATH"                      # Windows npm 전역
  export PATH="/c/Program Files/nodejs:$PATH"           # Windows 기본 설치
}

# 디버그 모드 활성화 (문제 해결 시 주석 제거)
# set -x
