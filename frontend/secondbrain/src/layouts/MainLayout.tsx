import { useState, type ReactNode } from 'react';
import { BaseLayout } from '@/layouts/BaseLayout';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import { UserProfileButton } from '@/features/auth/components/UserProfileButton';
import { NoteCreateModal } from '@/features/note/components/NoteCreateModal';
import PlusIcon from '@/shared/components/icon/Plus.svg?react';

interface MainLayoutProps {
  children: ReactNode;
}

const MainLayout = ({ children }: MainLayoutProps) => {
  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false);

  function openNoteModal() {
    setIsNoteModalOpen(true);
  }

  function closeNoteModal() {
    setIsNoteModalOpen(false);
  }

  function handleNoteSave(markdown: string) {
    // TODO: API 호출하여 노트 저장
    console.log('Note saved:', markdown);
  }

  function handleNoteDelete() {
    // TODO: 노트 삭제 확인 후 처리
    console.log('Note deleted');
  }

  return (
    <BaseLayout>
      {/* 배경 레이어: children (Graph 등) */}
      <div className="relative size-full">{children}</div>

      {/* UI 레이어: 개별 요소만 z-index로 위에 배치 */}
      <div className="absolute left-1/2 top-10 z-50 -translate-x-1/2">
        <GlassElement as="input" scale="md" />
      </div>

      <div className="absolute right-10 top-10 z-50">
        <UserProfileButton />
      </div>

      <div className="absolute bottom-10 right-10 z-50">
        <GlassElement as="button" icon={<PlusIcon />} onClick={openNoteModal} />
      </div>

      {/* Note 생성 모달 */}
      <NoteCreateModal
        isOpen={isNoteModalOpen}
        onClose={closeNoteModal}
        onSave={handleNoteSave}
        onDelete={handleNoteDelete}
      />
    </BaseLayout>
  );
};

export { MainLayout };
