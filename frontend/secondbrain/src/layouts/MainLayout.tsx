import { useState, useEffect, type ReactNode } from 'react';
import { BaseLayout } from '@/layouts/BaseLayout';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { UserProfileButton } from '@/features/auth/components/UserProfileButton';
import PlusIcon from '@/shared/components/icon/Plus.svg?react';
import SearchIcon from '@/shared/components/icon/Search.svg?react';
import MenuIcon from '@/shared/components/icon/Menu.svg?react';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { useDebounce } from '@/features/main/hooks/useDebounce';

interface MainLayoutProps {
  children: ReactNode;
  onPlusClick?: () => void;
}

const MainLayout = ({ children, onPlusClick }: MainLayoutProps) => {
  const openRecent = useSearchPanelStore((state) => state.openRecent);
  const closePanel = useSearchPanelStore((state) => state.closePanel);
  const updateQuery = useSearchPanelStore((state) => state.updateQuery);
  const mode = useSearchPanelStore((state) => state.mode);
  const isOpen = useSearchPanelStore((state) => state.isOpen);

  // 로컬 상태로 검색어 관리
  const [searchInput, setSearchInput] = useState('');
  // 300ms 디바운싱 적용
  const debouncedSearchInput = useDebounce(searchInput, 300);

  // 디바운싱된 값이 변경되면 store 업데이트 (패널이 열려있을 때만)
  useEffect(() => {
    if (mode !== 'closed') {
      updateQuery(debouncedSearchInput);
    }
  }, [debouncedSearchInput, updateQuery, mode]);

  // 햄버거 메뉴 클릭 핸들러: 패널 토글
  const handleMenuClick = () => {
    if (isOpen) {
      closePanel();
    } else {
      openRecent();
    }
  };

  return (
    <BaseLayout>
      {/* 배경 레이어: children (Graph 등) */}
      <div className="relative size-full">{children}</div>

      {/* UI 레이어: 개별 요소만 z-index로 위에 배치 */}
      {/* 햄버거 메뉴 버튼 - 패널이 닫혀있을 때만 표시 */}
      {!isOpen && (
        <div className="absolute left-10 top-10 z-50">
          <GlassElement
            as="button"
            icon={<MenuIcon />}
            onClick={handleMenuClick}
            aria-label="검색 패널 열기"
          />
        </div>
      )}

      <div className="absolute left-1/2 top-10 z-50 -translate-x-1/2">
        <GlassElement
          as="input"
          scale="md"
          icon={<SearchIcon />}
          placeholder="검색"
          value={searchInput}
          onFocus={openRecent}
          onChange={(e) => setSearchInput(e.target.value)}
        />
      </div>

      <div className="absolute right-10 top-10 z-50">
        <UserProfileButton />
      </div>

      <div className="absolute bottom-10 right-10 z-50">
        <GlassElement
          as="button"
          icon={<PlusIcon />}
          onClick={onPlusClick}
          aria-label="새 노트 작성"
        />
      </div>
    </BaseLayout>
  );
};

export { MainLayout };
