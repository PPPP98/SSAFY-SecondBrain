import { Crepe } from '@milkdown/crepe';
import '@milkdown/crepe/theme/common/style.css';
import '@milkdown/crepe/theme/frame.css';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import { listener, listenerCtx } from '@milkdown/plugin-listener';
import '@/features/note/components/NoteEditor.css';

interface NoteEditorProps {
  defaultValue?: string;
  onChange?: (markdown: string) => void;
}

function CrepeEditor({ defaultValue, onChange }: NoteEditorProps) {
  useEditor(
    (root) => {
      const crepe = new Crepe({
        root,
        defaultValue: defaultValue || '',
        featureConfigs: {
          [Crepe.Feature.Placeholder]: {
            text: '내용을 입력해주세요...',
            mode: 'block',
          },
        },
      });

      // onChange 이벤트 연결 (Milkdown listener plugin 사용)
      if (onChange) {
        crepe.editor.use(listener);

        crepe.editor.config((ctx) => {
          const listenerPlugin = ctx.get(listenerCtx);
          listenerPlugin.markdownUpdated((_ctx, markdown, prevMarkdown) => {
            if (markdown !== prevMarkdown) {
              onChange(markdown);
            }
          });
        });
      }

      return crepe;
    },
    [onChange],
  );

  return <Milkdown />;
}

/**
 * Milkdown 에디터 컴포넌트
 * - Crepe 기반 WYSIWYG 에디터
 * - Notion 스타일 Title/Content 구분
 * - 커스텀 스타일링 적용
 * - onChange 콜백 지원 (Draft 자동 저장용)
 */
export function NoteEditor({ defaultValue, onChange }: NoteEditorProps) {
  return (
    <div className="note-editor">
      <MilkdownProvider>
        <CrepeEditor defaultValue={defaultValue} onChange={onChange} />
      </MilkdownProvider>
    </div>
  );
}
