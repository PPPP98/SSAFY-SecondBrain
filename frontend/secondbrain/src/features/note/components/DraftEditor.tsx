import { useState, useEffect } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { toast } from 'sonner';
import { useNoteDraft } from '@/features/note/hooks/useNoteDraft';
import { SidePeekOverlay } from '@/features/note/components/SidePeekOverlay';
import { DraftToolbar } from '@/features/note/components/DraftToolbar';
import { NoteTitleInput } from '@/features/note/components/NoteTitleInput';
import { NoteEditor } from '@/features/note/components/NoteEditor';
import '@/shared/styles/custom-scrollbar.css';

interface DraftEditorProps {
  draftId: string;
  isOpen: boolean;
  onClose: () => void;
}

/**
 * Draft 에디터 (Side Peek)
 * - NoteTitleInput + NoteEditor 재사용
 * - 자동 저장 (useNoteDraft)
 * - Toolbar: 뒤로가기, 확대, 삭제
 */
export function DraftEditor({ draftId, isOpen, onClose }: DraftEditorProps) {
  const navigate = useNavigate();
  const [isAnimated, setIsAnimated] = useState(false);

  // 애니메이션 트리거
  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => setIsAnimated(true), 150);
      return () => clearTimeout(timer);
    } else {
      setIsAnimated(false);
    }
  }, [isOpen]);

  const { title, content, handleTitleChange, handleContentChange, saveToDatabase, deleteDraft } =
    useNoteDraft({
      draftId,
      onSaveToDatabase: (noteId) => {
        // DB 저장 후 /note로 이동
        void navigate({ to: '/note', search: { id: noteId } });
      },
    });

  const handleClose = async () => {
    // 유효한 Draft → DB 저장
    if (title.trim() && content.trim()) {
      try {
        await saveToDatabase();
        // saveToDatabase()가 성공하면:
        // - DB에 Note 저장
        // - Redis에서 Draft 삭제
        // - onSaveToDatabase 콜백 실행 → navigate({ to: '/note', search: { id: noteId } })
        // 따라서 여기서 onClose() 호출하지 않음
        return;
      } catch (error) {
        console.error('DB 저장 실패:', error);
        toast.error('노트 저장에 실패했습니다', {
          description: '작성하신 내용은 임시 저장되었어요. 잠시 후 다시 시도해주세요.',
        });
        // 에러 시 그냥 닫기 (Draft는 Redis에 유지)
      }
    }

    // 빈 Draft → Redis 삭제
    if (!title.trim() && !content.trim()) {
      try {
        await deleteDraft();
      } catch (error) {
        // Draft가 없어도 정상 (빈 노트는 저장 안 됨)
        console.error('Draft 삭제 실패:', error);
      }
    }

    onClose();
  };

  const handleDelete = () => {
    void deleteDraft();
    onClose();
  };

  return (
    <SidePeekOverlay isOpen={isOpen} onClose={() => void handleClose()}>
      <DraftToolbar onBack={() => void handleClose()} onDelete={handleDelete} />

      {/* 중앙 컨텐츠: Title + Editor (전체 스크롤) */}
      <div className="custom-scrollbar absolute inset-x-0 bottom-0 top-32 flex flex-col items-center gap-8 overflow-y-auto px-24">
        <div
          className={`flex w-full max-w-4xl flex-col transition-all duration-500 ease-out ${
            isAnimated ? 'translate-y-0 opacity-100' : 'translate-y-8 opacity-0'
          }`}
        >
          {/* 제목 입력 */}
          <NoteTitleInput
            value={title}
            onChange={handleTitleChange}
            placeholder="제목을 입력해주세요..."
          />

          {/* 마크다운 에디터 */}
          <div className="pb-20">
            <NoteEditor defaultValue={content} onChange={handleContentChange} />
          </div>
        </div>
      </div>
    </SidePeekOverlay>
  );
}
