import { useRef, useEffect } from 'react';
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
  // onChange를 ref로 저장하여 안정적인 참조 유지
  const onChangeRef = useRef(onChange);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

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

      // onChange 이벤트 연결 (ref 사용으로 에디터 재생성 방지)
      crepe.editor.use(listener);

      crepe.editor.config((ctx) => {
        const listenerPlugin = ctx.get(listenerCtx);
        listenerPlugin.markdownUpdated((_ctx, markdown, prevMarkdown) => {
          if (markdown !== prevMarkdown) {
            onChangeRef.current?.(markdown);
          }
        });
      });

      return crepe;
    },
    [], // onChange 의존성 제거 - ref로 최신 값 참조
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
