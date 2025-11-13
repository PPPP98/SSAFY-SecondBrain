import { GoogleLoginButton } from '@/content-scripts/overlay/GoogleLoginButton';

/**
 * Login Prompt (Molecule)
 * - 로그인 전 표시되는 프롬프트
 * - Shadcn UI + Tailwind CSS 기반
 */
export function LoginPrompt() {
  return (
    <div className="w-[320px] rounded-xl border border-border bg-card p-6 shadow-lg">
      {/* 로고 이미지 (회전 애니메이션) */}
      <div className="mb-6 flex justify-center">
        <img
          src={chrome.runtime.getURL('Logo_upscale.png')}
          alt="Second Brain Logo"
          className="h-[120px] w-[120px] animate-spin object-contain"
          style={{ animationDuration: '6s' }}
        />
      </div>

      <h3 className="mb-2 text-center text-lg font-semibold text-card-foreground">Second Brain</h3>

      <div className="flex justify-center">
        <GoogleLoginButton text="signin" />
      </div>
    </div>
  );
}
