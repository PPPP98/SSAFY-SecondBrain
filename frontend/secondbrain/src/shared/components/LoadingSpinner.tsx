/**
 * 로딩 스피너 컴포넌트
 * - 3D 뇌 모양 아이콘이 회전하는 로딩 인디케이터
 * - 배경색: #192030
 * - 아이콘: 하얀색
 */

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  message?: string;
}

export function LoadingSpinner({ size = 'md', message }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'w-16 h-16',
    md: 'w-24 h-24',
    lg: 'w-32 h-32',
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6">
      <div className={`${sizeClasses[size]} relative`} role="status" aria-label="Loading">
        <style>{`
          @keyframes rotate {
            from {
              transform: rotate(0deg);
            }
            to {
              transform: rotate(360deg);
            }
          }
          [data-brain-spinner] {
            animation: rotate 3s linear infinite;
          }
        `}</style>

        <svg
          data-brain-spinner
          className="size-full"
          viewBox="0 0 200 200"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Outer circle */}
          <circle
            cx="100"
            cy="100"
            r="90"
            fill="none"
            stroke="white"
            strokeWidth="0.5"
            opacity="0.3"
          />

          {/* Brain pattern - circular arrangement */}
          <path
            d="M 100 20
               Q 130 25, 150 50
               Q 170 75, 165 105
               Q 160 135, 140 155
               Q 120 175, 100 180
               Q 80 175, 60 155
               Q 40 135, 35 105
               Q 30 75, 50 50
               Q 70 25, 100 20

               M 100 40
               C 85 45, 75 55, 70 70
               M 100 40
               C 115 45, 125 55, 130 70

               M 70 70
               Q 65 85, 68 100
               M 130 70
               Q 135 85, 132 100

               M 68 100
               Q 70 115, 80 130
               M 132 100
               Q 130 115, 120 130

               M 80 130
               Q 90 145, 100 150
               M 120 130
               Q 110 145, 100 150

               M 100 60
               L 100 100
               M 80 80
               L 120 120
               M 120 80
               L 80 120"
            fill="none"
            stroke="white"
            strokeWidth="1"
            strokeLinecap="round"
            strokeLinejoin="round"
            opacity="0.8"
          />

          {/* Inner decorative circles */}
          <circle cx="100" cy="100" r="5" fill="white" opacity="0.6" />
          <circle cx="70" cy="70" r="3" fill="white" opacity="0.4" />
          <circle cx="130" cy="70" r="3" fill="white" opacity="0.4" />
          <circle cx="70" cy="130" r="3" fill="white" opacity="0.4" />
          <circle cx="130" cy="130" r="3" fill="white" opacity="0.4" />
        </svg>
      </div>
      {message && <p className="text-sm text-white/80">{message}</p>}
    </div>
  );
}
