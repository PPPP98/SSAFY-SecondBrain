import { Crepe } from '@milkdown/crepe';
import '@milkdown/crepe/theme/common/style.css';
import '@milkdown/crepe/theme/frame.css';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';

interface NoteEditorProps {
  defaultValue?: string;
  placeholder?: string;
}

function CrepeEditor({ defaultValue, placeholder }: NoteEditorProps) {
  useEditor((root) => {
    return new Crepe({
      root,
      defaultValue: defaultValue || '# 제목을 입력하세요\n\n내용을 작성하세요...',
      placeholder: placeholder || '내용을 입력하세요...',
    });
  });

  return <Milkdown />;
}

/**
 * Milkdown 에디터 컴포넌트
 * - Crepe 기반 WYSIWYG 에디터
 * - GlassElement 스타일 래핑
 * - Heading을 통한 Title 처리
 */
export function NoteEditor({ defaultValue, placeholder }: NoteEditorProps) {
  return (
    <GlassElement as="div" className="size-full overflow-auto">
      <div className="h-full p-6">
        <MilkdownProvider>
          <CrepeEditor defaultValue={defaultValue} placeholder={placeholder} />
        </MilkdownProvider>
      </div>
    </GlassElement>
  );
}
