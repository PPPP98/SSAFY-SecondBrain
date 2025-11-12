import { useState } from 'react';
import { useNavigate, useParams } from '@tanstack/react-router';
import { MainLayout } from '@/layouts/MainLayout';
import { Graph } from '@/features/main/components/Graph';
import { SidePeekOverlay } from '@/features/note/components/SidePeekOverlay';
import { DraftToolbar } from '@/features/note/components/DraftToolbar';
import { NoteTitleInput } from '@/features/note/components/NoteTitleInput';
import { NoteEditor } from '@/features/note/components/NoteEditor';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useNoteQuery } from '@/features/note/hooks/useNoteQuery';
import { useNoteDelete } from '@/features/note/hooks/useNoteDelete';
import '@/shared/styles/custom-scrollbar.css';

/**
 * Note 페이지 with SidePeekOverlay
 * - /notes/:noteId 경로에서 사용
 * - Graph 배경 + SidePeekOverlay로 노트 표시
 * - DraftEditor와 동일한 구조 재사용
 */
export function NoteViewPage() {
  const navigate = useNavigate();
  const { noteId } = useParams({ from: '/notes/$noteId' });
  const [viewMode, setViewMode] = useState<'full-screen' | 'side-peek'>('full-screen');

  // 노트 데이터 가져오기
  const { data: noteData, isLoading, isError } = useNoteQuery(noteId);

  // 노트 삭제 훅
  const { mutate: deleteNote, isPending: isDeleting } = useNoteDelete();

  const handleClose = () => {
    void navigate({ to: '/main' });
  };

  const handleToggleMode = () => {
    setViewMode((prev) => (prev === 'full-screen' ? 'side-peek' : 'full-screen'));
  };

  const handleDelete = () => {
    if (isDeleting) return;

    deleteNote(
      { noteIds: [Number(noteId)] },
      {
        onSuccess: () => {
          void navigate({ to: '/main' });
        },
        onError: (error) => {
          console.error('노트 삭제 실패:', error);
        },
      },
    );
  };

  const handleCreateDraft = () => {
    const draftId = crypto.randomUUID();
    void navigate({ to: '/main', search: { draft: draftId } });
  };

  return (
    <MainLayout onPlusClick={handleCreateDraft}>
      {/* 배경: Graph */}
      <Graph />

      {/* Side Peek: Note */}
      <SidePeekOverlay
        isOpen={true}
        onClose={handleClose}
        mode={viewMode}
        onToggleMode={handleToggleMode}
      >
        <DraftToolbar
          onBack={handleClose}
          onDelete={handleDelete}
          mode={viewMode}
          onToggleMode={handleToggleMode}
        />

        {/* 중앙 컨텐츠: Title + Editor (전체 스크롤) */}
        <div className="custom-scrollbar absolute inset-x-0 bottom-0 top-32 flex flex-col items-center gap-8 overflow-y-auto px-24">
          {isLoading ? (
            <LoadingSpinner />
          ) : isError || !noteData ? (
            <div className="flex h-full items-center justify-center">
              <p className="text-gray-500">노트를 불러올 수 없습니다.</p>
            </div>
          ) : (
            <div
              className="flex w-full max-w-4xl translate-y-0 flex-col opacity-100 transition-all duration-500 ease-out"
              style={{
                transitionDelay: '150ms',
              }}
            >
              {/* 제목 입력 */}
              <NoteTitleInput
                value={noteData.title}
                onChange={() => {}}
                placeholder="제목을 입력해주세요..."
              />

              {/* 마크다운 에디터 */}
              <div className="pb-20">
                <NoteEditor defaultValue={noteData.content} />
              </div>
            </div>
          )}
        </div>
      </SidePeekOverlay>
    </MainLayout>
  );
}
