import { useState, useEffect } from 'react';
import SearchIcon from '@/shared/components/icon/Search.svg?react';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { useDebounce } from '@/features/main/hooks/useDebounce';
import { useSearchNotes } from '@/features/main/hooks/useSearchNotes';

/**
 * 검색바 컴포넌트
 * - 검색 아이콘 + 밑줄 스타일
 * - 디바운싱을 통한 검색 쿼리 최적화
 * - 검색 결과가 있을 때만 패널 표시
 */
export function SearchBar() {
  const startSearch = useSearchPanelStore((state) => state.startSearch);
  const closePanel = useSearchPanelStore((state) => state.closePanel);
  const mode = useSearchPanelStore((state) => state.mode);

  // 로컬 상태로 검색어 관리
  const [searchInput, setSearchInput] = useState('');
  // 150ms 디바운싱 적용 (더 빠른 응답성)
  const debouncedSearchInput = useDebounce(searchInput, 150);

  // 검색 쿼리 실행 (enabled로 조건부 실행)
  const searchQuery = useSearchNotes({
    keyword: debouncedSearchInput,
  });

  // 검색 결과 기반 패널 표시 로직
  useEffect(() => {
    const trimmedInput = debouncedSearchInput.trim();

    if (!trimmedInput) {
      // 검색어 없음 → search 모드에서만 패널 닫기 (recent 모드는 유지)
      if (mode === 'search') {
        closePanel();
      }
      return;
    }

    // 검색 시작 (로딩 중이거나 성공한 경우 패널 열기)
    if (searchQuery.isLoading || searchQuery.isSuccess) {
      // 검색 패널 즉시 열기 (로딩 중에도 표시)
      startSearch(debouncedSearchInput);
    }
    // isError 상태에서는 아무 동작 안 함 (패널 상태 유지)
  }, [
    debouncedSearchInput,
    searchQuery.isLoading,
    searchQuery.isSuccess,
    mode,
    closePanel,
    startSearch,
  ]);

  return (
    <div className="flex items-center gap-3">
      <SearchIcon className="text-white/80" />
      <input
        type="text"
        placeholder="검색"
        aria-label="검색"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        className="w-[300px] border-b-2 border-white/60 bg-transparent pb-2 text-white outline-none transition-colors placeholder:text-white/50 focus:border-white"
      />
    </div>
  );
}
