import { useLogout } from '@/features/auth/hooks/useLogout';

/**
 * 로그아웃 버튼 컴포넌트
 * - useLogout 훅을 사용하여 로그아웃 처리
 * - 로딩 상태 표시
 */

interface LogoutButtonProps {
  variant?: 'primary' | 'secondary';
  size?: 'sm' | 'md' | 'lg';
}

export function LogoutButton({ variant = 'secondary', size = 'md' }: LogoutButtonProps) {
  const { mutate: logout, isPending } = useLogout();

  function handleLogout() {
    logout();
  }

  const variantClasses = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
    secondary: 'bg-gray-200 text-gray-900 hover:bg-gray-300 focus:ring-gray-500',
  };

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-6 py-3 text-lg',
  };

  return (
    <button
      onClick={handleLogout}
      disabled={isPending}
      className={`rounded font-medium focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-50 ${variantClasses[variant]} ${sizeClasses[size]}`}
    >
      {isPending ? '로그아웃 중...' : '로그아웃'}
    </button>
  );
}
