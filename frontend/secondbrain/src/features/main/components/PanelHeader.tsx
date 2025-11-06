import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import GlassElement from '@/shared/components/GlassElement/GlassElement';
import DoubleArrow from '@/shared/components/icon/DoubleArrow.svg?react';
import CheckBoxIcon from '@/shared/components/icon/CheckBox.svg?react';
import DeleteIcon from '@/shared/components/icon/Delete.svg?react';

interface PanelHeaderProps {
  allNoteIds: number[];
}

export function PanelHeader({ allNoteIds }: PanelHeaderProps) {
  const closePanel = useSearchPanelStore((state) => state.closePanel);
  const selectedIds = useSearchPanelStore((state) => state.selectedIds);
  const selectAll = useSearchPanelStore((state) => state.selectAll);
  const deselectAll = useSearchPanelStore((state) => state.deselectAll);

  // 전체 선택 여부 계산
  const isAllSelected = allNoteIds.length > 0 && allNoteIds.every((id) => selectedIds.has(id));
  const hasSelection = selectedIds.size > 0;
  const handleSelectAll = () => {
    if (isAllSelected) {
      deselectAll();
    } else {
      selectAll(allNoteIds);
    }
  };

  const handleDelete = () => {
    if (!hasSelection) return;

    const noteIdsArray = Array.from(selectedIds);
    // 삭제 함수 연결
    console.log('삭제할 노트 IDs:', noteIdsArray);
  };

  return (
    <div className="flex items-center justify-between border-b border-white/20 p-4">
      <div className="flex items-center gap-3">
        {/* 전체 선택 체크박스 */}
        <button
          onClick={handleSelectAll}
          className={`flex size-5 items-center justify-center rounded border-2 transition-all ${
            isAllSelected
              ? 'border-white bg-white'
              : hasSelection
                ? 'border-white/60 bg-white/20'
                : 'border-white/40 bg-transparent hover:border-white/60'
          }`}
          aria-label="전체 선택"
        >
          {isAllSelected && <CheckBoxIcon className="size-4 text-black" />}
        </button>

        {/* 선택된 항목 수 및 삭제 버튼 */}
        {hasSelection && (
          <GlassElement
            as="button"
            icon={<DeleteIcon />}
            onClick={handleDelete}
            // disabled={deleteMutation.isPending} 삭제 훅 연결 후 활성화
            className="px-3 py-1.5 text-sm font-medium text-white"
          />
        )}
      </div>

      {/* 닫기 버튼 */}
      <button
        onClick={closePanel}
        className="flex size-8 items-center justify-center rounded-lg text-white/80 transition-colors hover:bg-white/10 hover:text-white"
      >
        <DoubleArrow />
      </button>
    </div>
  );
}
