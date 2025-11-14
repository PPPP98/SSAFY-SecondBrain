import React, { useEffect, useState } from 'react';
import { Search, X } from 'lucide-react';
import type { FloatingButtonPosition } from '@/types/dragSearch';

interface FloatingSearchButtonProps {
  position: FloatingButtonPosition;
  keyword: string;
  onSearch: () => void;
  onClose: () => void;
  autoHideMs: number;
}

/**
 * 드래그 텍스트 검색 플로팅 버튼
 * 드래그 위치 근처에 표시되며, 클릭 시 검색 실행
 */
export const FloatingSearchButton: React.FC<FloatingSearchButtonProps> = ({
  position,
  keyword,
  onSearch,
  onClose,
  autoHideMs,
}) => {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    // 자동 숨김 타이머
    const timer = setTimeout(() => {
      setIsVisible(false);
      onClose();
    }, autoHideMs);

    return () => clearTimeout(timer);
  }, [autoHideMs, onClose]);

  if (!isVisible) return null;

  return (
    <div
      className="fixed z-[999999] flex animate-in items-center gap-2 rounded-lg bg-white shadow-lg ring-1 ring-black/5 transition-all duration-200 fade-in slide-in-from-bottom-2 hover:shadow-xl"
      style={{
        left: `${position.x}px`,
        top: `${position.y + 10}px`, // 드래그 위치 바로 아래
      }}
    >
      <button
        onClick={onSearch}
        className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:text-blue-600"
        title={`"${keyword}" 검색`}
      >
        <Search className="h-4 w-4 flex-shrink-0" />
        <span className="max-w-[200px] truncate">노트 검색: {keyword}</span>
      </button>
      <button
        onClick={onClose}
        className="px-2 py-2 text-gray-400 transition-colors hover:text-gray-600"
        title="닫기"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};
