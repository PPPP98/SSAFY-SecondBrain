import { useEffect, useState } from 'react';
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
export function FloatingSearchButton({
  position,
  keyword,
  onSearch,
  onClose,
  autoHideMs,
}: FloatingSearchButtonProps) {
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
      className="floating-button"
      style={{
        left: `${position.x}px`,
        top: `${position.y + 10}px`, // 드래그 위치 바로 아래
      }}
    >
      <button onClick={onSearch} className="search-button" title={`"${keyword}" 검색`}>
        <Search className="icon" />
        <span className="keyword-text">노트 검색: {keyword}</span>
      </button>
      <button onClick={onClose} className="close-button" title="닫기">
        <X className="icon" />
      </button>
    </div>
  );
}
