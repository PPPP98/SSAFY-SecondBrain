import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
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
    <div className="mb-3 flex items-center justify-between">
      <div className="flex items-center gap-3">
        {/* 전체 선택 체크박스 */}
        <GlassElement
          as="button"
          onClick={handleSelectAll}
          className="flex items-center justify-center"
          aria-label="전체 선택"
        >
          <div
            className={`flex size-4 items-center justify-center rounded-sm border-2 transition-all ${
              isAllSelected
                ? 'border-white bg-white'
                : 'border-white/80 bg-transparent hover:border-white'
            }`}
          >
            {isAllSelected && (
              <div className="size-full bg-white">
                <CheckBoxIcon className="size-full text-black" />
              </div>
            )}
          </div>
        </GlassElement>

        {/* 선택된 항목 수 및 삭제 버튼 */}
        {hasSelection && (
          <GlassElement
            as="button"
            icon={<DeleteIcon />}
            onClick={handleDelete}
            // disabled={deleteMutation.isPending} 삭제 훅 연결 후 활성화
            className="text-white"
          />
        )}
      </div>

      {/* 닫기 버튼 */}
      <GlassElement as="button" onClick={closePanel} className="flex items-center justify-center">
        <DoubleArrow className="text-white/80" />
      </GlassElement>
    </div>
  );
}
