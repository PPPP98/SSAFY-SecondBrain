import { useState } from 'react';
import { Plus, Download } from 'lucide-react';
import { Button } from '@/content-scripts/overlay/components/ui/button';
import { CounterBadge } from '@/content-scripts/overlay/components/atoms/CounterBadge';
import { URLListModal } from '@/content-scripts/overlay/components/organisms/URLListModal';
import { SaveStatusPanel } from '@/content-scripts/overlay/components/organisms/SaveStatusPanel';
import { usePageCollectionStore } from '@/stores/pageCollectionStore';
import { useSaveStatusStore } from '@/stores/saveStatusStore';
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
 * - 페이지 추가, 저장 기능
 * - 비동기 요청 큐 관리로 여러 저장 요청 동시 처리
 * - Shadcn UI + Tailwind CSS 기반
 */
export function ActionButtons() {
  const [showURLList, setShowURLList] = useState(false);
  const [showSaveStatus, setShowSaveStatus] = useState(false);

  const { pages, addPage, removePage, clearPages, getPageList } = usePageCollectionStore();
  const { addSaveRequest, updateSaveStatus, getSavingCount } = useSaveStatusStore();

  async function handleAddPage(): Promise<void> {
    const currentUrl = window.location.href;

    // URL 유효성 검증
    if (!currentUrl.startsWith('http://') && !currentUrl.startsWith('https://')) {
      showToast('이 페이지는 추가할 수 없습니다', 'error');
      return;
    }

    // 중복 체크 및 추가 (비동기)
    const success = await addPage(currentUrl);

    if (success) {
      showToast('페이지가 추가되었습니다', 'success');
    } else {
      showToast('이미 추가된 페이지입니다', 'info');
    }
  }

  async function handleSave(): Promise<void> {
    try {
      const urlsToSave = getPageList();
      const currentUrl = window.location.href;

      // 수집된 페이지가 있으면 전체 저장, 없으면 현재 페이지만 저장
      const finalUrls = urlsToSave.length > 0 ? urlsToSave : [currentUrl];

      // 1. 각 URL을 Store에 'saving' 상태로 추가
      const requestIds = finalUrls.map((url) => addSaveRequest(url));

      // 2. 패널 자동 열기
      setShowSaveStatus(true);

      // 3. Background Service Worker에 메시지 전송 (URLs 배열 포함)
      const rawResponse: unknown = await browser.runtime.sendMessage({
        type: 'SAVE_CURRENT_PAGE',
        urls: finalUrls,
      });

      // 응답 타입 검증
      const response = rawResponse as SavePageResponse | SavePageError;

      // 4. 응답에 따라 상태 업데이트
      if ('error' in response) {
        // 에러 발생 - 모든 요청을 error 상태로
        console.error('Save failed:', response);
        const errorMessage = getErrorMessage(response.error);
        requestIds.forEach((id) => {
          updateSaveStatus(id, 'error', errorMessage);
        });
        showToast(`저장 실패: ${errorMessage}`, 'error');
      } else {
        // 성공 - 모든 요청을 success 상태로
        requestIds.forEach((id) => {
          updateSaveStatus(id, 'success');
        });

        const savedCount = finalUrls.length;
        showToast(
          savedCount > 1
            ? `${savedCount}개 페이지가 저장되었습니다`
            : '페이지가 저장되었습니다',
          'success',
        );

        // 성공 시 수집 목록 초기화
        if (urlsToSave.length > 0) {
          void clearPages();
          setShowURLList(false);
        }
      }
    } catch (error) {
      // 예외 처리 (Background 통신 실패)
      console.error('Failed to save page:', error);
      showToast('저장 실패: 확장 프로그램 오류', 'error');
    }
  }

  const savingCount = getSavingCount();
  const pageCount = pages.size;

  return (
    <div className="relative w-[260px] space-y-2 rounded-xl border border-border bg-card p-4 shadow-lg">
      {/* Add Button + Counter Badge */}
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          className="flex-1 justify-start gap-2 hover:bg-accent"
          onClick={() => void handleAddPage()}
        >
          <Plus className="h-4 w-4" />
          <span>Add</span>
        </Button>

        <CounterBadge count={pageCount} onClick={() => setShowURLList(!showURLList)} />
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

        {/* Save Status Toggle Button */}
        {savingCount > 0 && (
          <CounterBadge
            count={savingCount}
            onClick={() => setShowSaveStatus(!showSaveStatus)}
          />
        )}

        {/* Save Status Panel */}
        <SaveStatusPanel isOpen={showSaveStatus} onClose={() => setShowSaveStatus(false)} />
      </div>

      {/* URL List Modal */}
      <URLListModal
        isOpen={showURLList}
        onClose={() => setShowURLList(false)}
        urls={getPageList()}
        onRemove={(url: string) => void removePage(url)}
        onClearAll={() => {
          void clearPages();
          setShowURLList(false);
        }}
      />
    </div>
  );
}
