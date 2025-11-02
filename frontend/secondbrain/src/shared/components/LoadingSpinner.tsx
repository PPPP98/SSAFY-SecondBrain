/**
 * 로딩 스피너 컴포넌트
 * - 전역적으로 사용 가능한 로딩 인디케이터
 */

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  message?: string;
}

export function LoadingSpinner({ size = 'md', message }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'h-8 w-8 border-2',
    md: 'h-12 w-12 border-4',
    lg: 'h-16 w-16 border-4',
  };

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4">
      <div
        className={`animate-spin rounded-full border-blue-600 border-t-transparent ${sizeClasses[size]}`}
        role="status"
        aria-label="Loading"
      />
      {message && <p className="text-gray-600">{message}</p>}
    </div>
  );
}
