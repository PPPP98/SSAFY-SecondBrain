import { SlideOverModal } from '@/shared/components/SlideOverModal/SlideOverModal';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { NoteEditor } from '@/features/note/components/NoteEditor';
import BackArrowIcon from '@/shared/components/icon/BackArrow.svg?react';
import DeleteIcon from '@/shared/components/icon/Delete.svg?react';
import CheckBoxIcon from '@/shared/components/icon/CheckBox.svg?react';

interface NoteCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave?: (markdown: string) => void;
  onDelete?: () => void;
}

/**
 * Note 생성 모달 컴포넌트
 * - SlideOverModal + Milkdown 에디터 조합
 * - NoteLayout 버튼 구조 참고
 * - Notion Side Peek 스타일
 */
export function NoteCreateModal({ isOpen, onClose, onSave, onDelete }: NoteCreateModalProps) {
  function handleSave() {
    // TODO: 에디터 내용 가져와서 저장
    if (onSave) {
      onSave('# 제목\n\n내용');
    }
    onClose();
  }

  function handleDelete() {
    if (onDelete) {
      onDelete();
    }
    onClose();
  }

  return (
    <SlideOverModal isOpen={isOpen} onClose={onClose} width="50%" direction="right">
      <div className="relative h-full bg-[#192030]">
        {/* 상단 좌측: 닫기 버튼 */}
        <div className="absolute left-10 top-10 z-10">
          <GlassElement as="button" icon={<BackArrowIcon />} onClick={onClose} />
        </div>

        {/* 상단 우측: 삭제 버튼 */}
        <div className="absolute right-10 top-10 z-10">
          <GlassElement as="button" icon={<DeleteIcon />} onClick={handleDelete} />
        </div>

        {/* 하단 우측: 저장 버튼 */}
        <div className="absolute bottom-10 right-10 z-10">
          <GlassElement as="button" icon={<CheckBoxIcon />} onClick={handleSave} />
        </div>

        {/* 에디터 영역 */}
        <div className="h-full p-20">
          <NoteEditor
            defaultValue="# 제목을 입력하세요\n\n내용을 작성하세요..."
            placeholder="내용을 입력하세요..."
          />
        </div>
      </div>
    </SlideOverModal>
  );
}
