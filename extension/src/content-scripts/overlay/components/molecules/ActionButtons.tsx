import { useState, useEffect } from 'react';
import { Plus, Download, Settings, FileText } from 'lucide-react';
import { Button } from '@/content-scripts/overlay/components/ui/button';
import { CounterBadge } from '@/content-scripts/overlay/components/atoms/CounterBadge';
import { SearchInput } from '@/content-scripts/overlay/components/atoms/SearchInput';
import { usePageCollectionStore } from '@/stores/pageCollectionStore';
import { usePendingTextSnippetsStore } from '@/stores/pendingTextSnippetsStore';
import { useSaveStatusStore } from '@/stores/saveStatusStore';
import { useAiSearchStore } from '@/stores/aiSearchStore';
import browser from 'webextension-polyfill';
import { showToast } from '@/content-scripts/overlay/components/molecules/SimpleToast';
import type { SavePageResponse, SavePageError } from '@/types/note';

/**
 * 에러 코드를 사용자 친화적 메시지로 변환
 */
function getErrorMessage(errorCode: string): string {
  const errorMessages: Record<string, string> = {
    NO_TOKEN: '로그인이 필요합니다',
    NO_TAB: '현재 탭을 찾을 수 없습니다',
    INVALID_URL: '이 페이지는 저장할 수 없습니다',
    API_ERROR: 'API 오류가 발생했습니다',
    NETWORK_ERROR: '네트워크 연결을 확인해주세요',
    UNKNOWN_ERROR: '알 수 없는 오류가 발생했습니다',
  };

  return errorMessages[errorCode] || errorMessages['UNKNOWN_ERROR'];
}

/**
 * Action Buttons (Molecule)
 * - 로그인 후 표시되는 액션 버튼들
 * - 검색바 + 페이지 추가, 저장 기능
 * - Shadcn UI + Tailwind CSS 기반
 */

type ActivePanel = null | 'urlList' | 'snippetsList' | 'saveStatus' | 'settings' | 'aiSearch';

interface ActionButtonsProps {
  activePanel: ActivePanel;
  onTogglePanel: (
    panel: 'urlList' | 'snippetsList' | 'saveStatus' | 'settings' | 'aiSearch',
  ) => void;
}

export function ActionButtons({ activePanel, onTogglePanel }: ActionButtonsProps) {
  const { pages, addPage, clearPages } = usePageCollectionStore();
  const { getSnippetCount } = usePendingTextSnippetsStore();
  const { getSavingCount } = useSaveStatusStore();
  const { keyword, setKeyword, search, clearSearch, setFocused } = useAiSearchStore();

  const [isSearchMode, setIsSearchMode] = useState(false);

  useEffect(() => {
    if (activePanel === 'aiSearch') {
      setIsSearchMode(true);
    } else if (activePanel !== null) {
      setIsSearchMode(false);
    }
  }, [activePanel]);

  function handleSearchFocus() {
    setIsSearchMode(true);
    setFocused(true);
  }

  function handleSearchCancel() {
    setIsSearchMode(false);
    setFocused(false);
    clearSearch();
    onTogglePanel(null as never);
  }

  function handleSearch() {
    if (keyword.trim()) {
      onTogglePanel('aiSearch');
      void search(keyword.trim());
    }
  }

  async function handleAddPage(): Promise<void> {
    const currentUrl = window.location.href;

    if (!currentUrl.startsWith('http://') && !currentUrl.startsWith('https://')) {
      showToast('이 페이지는 추가할 수 없습니다', 'error');
      return;
    }

    const success = await addPage(currentUrl);

    if (success) {
      showToast('페이지가 추가되었습니다', 'success');
    } else {
      showToast('이미 추가된 페이지입니다', 'info');
    }
  }

  async function handleSave(): Promise<void> {
    try {
      const MAX_SAVE_COUNT = 20;
      const urlsToSave = Array.from(pages);
      const snippets = usePendingTextSnippetsStore.getState().snippets;
      const currentUrl = window.location.href;

      const formattedSnippets = snippets.map((snippet) => {
        return `=== 출처 정보 ===
URL: ${snippet.sourceUrl}
제목: ${snippet.pageTitle}
추가 시간: ${new Date(snippet.timestamp).toLocaleString('ko-KR')}

=== 선택된 텍스트 ===
${snippet.text}`;
      });

      let finalUrls = [...urlsToSave, ...formattedSnippets];

      if (finalUrls.length === 0) {
        finalUrls = [currentUrl];
      }

      if (finalUrls.length > MAX_SAVE_COUNT) {
        showToast(`한 번에 최대 ${MAX_SAVE_COUNT}개까지 저장할 수 있습니다`, 'info');
        finalUrls = finalUrls.slice(0, MAX_SAVE_COUNT);
      }

      const batchId = `batch_${Date.now()}`;
      const batchTimestamp = Date.now();

      if (activePanel !== 'saveStatus') {
        onTogglePanel('saveStatus');
      }

      const rawResponse: unknown = await browser.runtime.sendMessage({
        type: 'SAVE_CURRENT_PAGE',
        urls: finalUrls,
        batchId,
        batchTimestamp,
      });

      const response = rawResponse as SavePageResponse | SavePageError;

      if ('error' in response) {
        const errorMessage = getErrorMessage(response.error);
        showToast(`저장 실패: ${errorMessage}`, 'error');
      } else {
        const savedCount = finalUrls.length;
        showToast(
          savedCount > 1 ? `${savedCount}개 항목이 저장되었습니다` : '저장되었습니다',
          'success',
        );

        await clearPages();
        await usePendingTextSnippetsStore.getState().clearSnippets();
      }
    } catch (error) {
      console.error('Failed to save page:', error);
      showToast('저장 실패: 확장 프로그램 오류', 'error');
    }
  }

  const savingCount = getSavingCount();
  const pageCount = pages.size;
  const snippetCount = getSnippetCount();

  return (
    <div
      className={`relative rounded-xl border border-border bg-card p-4 shadow-lg transition-all duration-300 ${
        isSearchMode ? 'w-[400px]' : 'w-[320px]'
      }`}
    >
      {/* 검색바 (최상단, 항상 표시) */}
      <div className="mb-3">
        <SearchInput
          value={keyword}
          onChange={setKeyword}
          onFocus={handleSearchFocus}
          onBlur={() => setFocused(false)}
          onSearch={handleSearch}
          onCancel={handleSearchCancel}
          placeholder="무엇이든 물어보세요..."
        />
      </div>

      {/* 기존 버튼들 - 검색 모드가 아닐 때만 렌더링 (공백 제거!) */}
      {!isSearchMode && (
        <div className="space-y-2">
          {/* Add Button + Counter Badge */}
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              className="flex-1 justify-start gap-2 hover:bg-accent"
              onClick={() => void handleAddPage()}
            >
              <Plus className="h-4 w-4" />
              <span>Add URL</span>
            </Button>

            <CounterBadge count={pageCount} onClick={() => onTogglePanel('urlList')} />
          </div>

          {/* Snippets Button + Counter Badge */}
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              className="flex-1 justify-start gap-2 hover:bg-accent"
              onClick={() => onTogglePanel('snippetsList')}
            >
              <FileText className="h-4 w-4" />
              <span>임시 노트</span>
            </Button>

            <CounterBadge count={snippetCount} onClick={() => onTogglePanel('snippetsList')} />
          </div>

          {/* Save Button + Status Toggle */}
          <div className="relative flex items-center gap-2">
            <Button
              variant="outline"
              className="flex-1 justify-start gap-2 hover:bg-accent"
              onClick={() => void handleSave()}
            >
              <Download className="h-4 w-4" />
              <span>Save</span>
            </Button>

            {savingCount > 0 && (
              <CounterBadge count={savingCount} onClick={() => onTogglePanel('saveStatus')} />
            )}
          </div>

          {/* Settings Button */}
          <Button
            variant="outline"
            className="w-full justify-start gap-2 hover:bg-accent"
            onClick={() => onTogglePanel('settings')}
          >
            <Settings className="h-4 w-4" />
            <span>드래그 검색 설정</span>
          </Button>
        </div>
      )}
    </div>
  );
}
