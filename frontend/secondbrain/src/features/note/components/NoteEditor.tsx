import { Crepe } from '@milkdown/crepe';
import '@milkdown/crepe/theme/common/style.css';
import '@milkdown/crepe/theme/frame.css';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import '@/features/note/components/NoteEditor.css';

interface NoteEditorProps {
  defaultValue?: string;
}

function CrepeEditor({ defaultValue }: NoteEditorProps) {
  useEditor((root) => {
    return new Crepe({
      root,
      defaultValue: defaultValue || '',
    });
  });

  return <Milkdown />;
}

/**
 * Milkdown 에디터 컴포넌트
 * - Crepe 기반 WYSIWYG 에디터
 * - Notion 스타일 Title/Content 구분
 * - 커스텀 스타일링 적용
 */
export function NoteEditor({ defaultValue }: NoteEditorProps) {
  return (
    <div className="note-editor">
      <MilkdownProvider>
        <CrepeEditor defaultValue={defaultValue} />
      </MilkdownProvider>
    </div>
  );
}
