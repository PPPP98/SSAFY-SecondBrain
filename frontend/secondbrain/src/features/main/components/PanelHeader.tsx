import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import DoubleArrow from '@/shared/components/icon/DoubleArrow.svg?react';
import DeleteIcon from '@/shared/components/icon/Delete.svg?react';

export function PanelHeader() {
  const closePanel = useSearchPanelStore((state) => state.closePanel);
  const selectedIds = useSearchPanelStore((state) => state.selectedIds);

  const hasSelection = selectedIds.size > 0;

  const handleDelete = () => {
    if (!hasSelection) return;

    const noteIdsArray = Array.from(selectedIds);
    // 삭제 함수 연결
    console.log('삭제할 노트 IDs:', noteIdsArray);
  };

  return (
    <div className="mb-6 flex items-center justify-between">
      {/* 삭제 버튼 */}
      <button
        onClick={handleDelete}
        className={`text-white transition-opacity ${hasSelection ? 'opacity-100 hover:opacity-80' : 'opacity-40'}`}
        aria-label="선택 항목 삭제"
      >
        <DeleteIcon className="size-6" />
      </button>

      {/* 닫기 버튼 */}
      <button
        onClick={closePanel}
        className="text-white/80 transition-colors hover:text-white"
        aria-label="패널 닫기"
      >
        <DoubleArrow className="size-5" />
      </button>
    </div>
  );
}
