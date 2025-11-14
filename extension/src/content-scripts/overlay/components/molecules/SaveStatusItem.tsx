import { useEffect, useState } from 'react';
import { Loader2, CheckCircle, XCircle } from 'lucide-react';
import { cn } from '@/lib/utils/utils';
import type { SaveRequest } from '@/types/save';

interface SaveStatusItemProps {
  request: SaveRequest;
  onRemove: (id: string) => void;
}

/**
 * 개별 저장 상태 아이템 (Molecule)
 * - 상태별 아이콘 표시 (Loading/Success/Error)
 * - Success 상태일 때 2초 후 자동 제거 (페이드아웃 애니메이션)
 * - Shadow DOM 환경 최적화
 */
export function SaveStatusItem({ request, onRemove }: SaveStatusItemProps) {
  const [isRemoving, setIsRemoving] = useState(false);

  // 성공 시 자동 제거 로직
  useEffect(() => {
    if (request.status === 'success') {
      // 2초간 성공 상태 표시
      const displayTimer = setTimeout(() => {
        // 페이드아웃 시작
        setIsRemoving(true);

        // 500ms 후 Store에서 제거
        setTimeout(() => {
          onRemove(request.id);
        }, 500);
      }, 2000);

      return () => clearTimeout(displayTimer);
    }
  }, [request.status, request.id, onRemove]);

  // 상태별 설정
  const statusConfig = {
    pending: {
      icon: Loader2,
      iconClass: 'animate-spin text-blue-500',
      bgClass: 'bg-blue-50 dark:bg-blue-950/30',
    },
    saving: {
      icon: Loader2,
      iconClass: 'animate-spin text-blue-500',
      bgClass: 'bg-blue-50 dark:bg-blue-950/30',
    },
    success: {
      icon: CheckCircle,
      iconClass: 'text-green-500',
      bgClass: 'bg-green-50 dark:bg-green-950/30',
    },
    error: {
      icon: XCircle,
      iconClass: 'text-red-500',
      bgClass: 'bg-red-50 dark:bg-red-950/30',
    },
  };

  const config = statusConfig[request.status];
  const Icon = config.icon;

  return (
    <div
      className={cn(
        'flex items-center gap-2 rounded-md border border-border p-2 transition-all duration-500',
        config.bgClass,
        isRemoving && 'opacity-0 translate-x-4'
      )}
    >
      <Icon className={cn('h-4 w-4 flex-shrink-0', config.iconClass)} />
      <span className="flex-1 truncate text-xs text-foreground" title={request.url}>
        {request.url}
      </span>
      {request.error && (
        <span className="text-xs text-red-500" title={request.error}>
          ⚠
        </span>
      )}
    </div>
  );
}
