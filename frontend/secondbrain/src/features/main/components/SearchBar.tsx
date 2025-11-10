import { useState, useEffect } from 'react';
import SearchIcon from '@/shared/components/icon/Search.svg?react';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { useDebounce } from '@/features/main/hooks/useDebounce';

/**
 * 검색바 컴포넌트
 * - 검색 아이콘 + 밑줄 스타일
 * - 디바운싱을 통한 검색 쿼리 최적화
 * - 포커스 시 검색 패널 자동 열기
 */
export function SearchBar() {
  const openRecent = useSearchPanelStore((state) => state.openRecent);
  const updateQuery = useSearchPanelStore((state) => state.updateQuery);
  const mode = useSearchPanelStore((state) => state.mode);

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

  return (
    <div className="flex items-center gap-3">
      <SearchIcon className="text-white/80" />
      <input
        type="text"
        placeholder="검색"
        aria-label="검색"
        value={searchInput}
        onFocus={openRecent}
        onChange={(e) => setSearchInput(e.target.value)}
        className="w-[300px] border-b-2 border-white/60 bg-transparent pb-2 text-white outline-none transition-colors placeholder:text-white/50 focus:border-white"
      />
    </div>
  );
}
