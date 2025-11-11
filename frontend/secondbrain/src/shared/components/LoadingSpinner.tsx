import logoSvg from '@/shared/components/icon/Logo.svg';

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
          @keyframes rotate3d {
            from {
              transform: rotateY(0deg);
            }
            to {
              transform: rotateY(360deg);
            }
          }
          .logo-spinner-container {
            perspective: 1000px;
          }
          .logo-spinner {
            animation: rotate3d 3s linear infinite;
            transform-style: preserve-3d;
          }
        `}</style>

        <div className="logo-spinner-container size-full">
          <img src={logoSvg} alt="Loading" className="logo-spinner size-full object-contain" />
        </div>
      </div>
      {message && <p className="text-sm text-white/80">{message}</p>}
    </div>
  );
}
