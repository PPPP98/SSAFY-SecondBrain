import { useEffect, useState } from 'react';
import { Loader2, FileText } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import browser from 'webextension-polyfill';
import type { NoteDetail } from '@/types/aiSearch';
import { getNoteDetail } from '@/services/aiSearchService';
import 'highlight.js/styles/github-dark.css';

/**
 * Side Panel 메인 앱
 * - Chrome Side Panel에서 노트 전체 내용 표시
 * - 마크다운 렌더링 (react-markdown)
 * - 다크모드 지원
 */
export function SidePanelApp() {
  const [noteId, setNoteId] = useState<number | null>(null);
  const [note, setNote] = useState<NoteDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  // Storage에서 noteId 및 theme 읽기
  useEffect(() => {
    // 초기 로드
    void browser.storage.local.get(['currentNoteId', 'theme']).then((result) => {
      if (result.currentNoteId) {
        setNoteId(result.currentNoteId as number);
      }
      if (result.theme) {
        setTheme(result.theme as 'light' | 'dark');
      }
    });

    // Storage 변경 감지 (다른 노트로 전환 시)
    const handleStorageChange = (
      changes: Record<string, browser.Storage.StorageChange>,
      areaName: string,
    ) => {
      if (areaName === 'local') {
        if (changes.currentNoteId) {
          setNoteId(changes.currentNoteId.newValue as number);
        }
        if (changes.theme) {
          setTheme(changes.theme.newValue as 'light' | 'dark');
        }
      }
    };

    browser.storage.onChanged.addListener(handleStorageChange);

    return () => {
      browser.storage.onChanged.removeListener(handleStorageChange);
    };
  }, []);

  // 다크모드 클래스 적용
  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  // Note 데이터 가져오기
  useEffect(() => {
    if (noteId === null) return;

    async function fetchNote() {
      if (noteId === null) return;

      setIsLoading(true);
      setError(null);

      try {
        const data = await getNoteDetail(noteId);
        setNote(data);
      } catch (err) {
        console.error('[SidePanelApp] Failed to fetch note:', err);
        setError(err instanceof Error ? err.message : '노트를 불러올 수 없습니다');
      } finally {
        setIsLoading(false);
      }
    }

    void fetchNote();
  }, [noteId]);

  // 로딩 상태
  if (isLoading || !note) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">노트를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="flex h-screen items-center justify-center bg-background p-6 text-center">
        <div className="flex flex-col items-center gap-3">
          <div className="rounded-full bg-red-50 p-3 dark:bg-red-950">
            <FileText className="h-12 w-12 text-red-500" />
          </div>
          <div>
            <p className="mb-2 text-base font-medium text-foreground">오류 발생</p>
            <p className="text-sm text-muted-foreground">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  // 노트 표시
  return (
    <div className="flex h-screen flex-col bg-background">
      {/* Header */}
      <div className="flex-shrink-0 border-b border-border bg-card p-4">
        <h1 className="mb-1 text-xl font-semibold text-foreground">{note.title}</h1>
        <div className="text-xs text-muted-foreground">
          생성: {new Date(note.createdAt).toLocaleDateString('ko-KR')}
          {note.updatedAt && note.updatedAt !== note.createdAt && (
            <> · 수정: {new Date(note.updatedAt).toLocaleDateString('ko-KR')}</>
          )}
        </div>
      </div>

      {/* Content - Markdown 렌더링 */}
      <div className="scrollbar-custom flex-1 overflow-y-auto p-6">
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          rehypePlugins={[rehypeHighlight]}
          className="prose prose-sm dark:prose-invert prose-headings:text-foreground prose-p:text-foreground prose-a:text-primary prose-a:no-underline hover:prose-a:underline prose-strong:text-foreground prose-code:text-foreground prose-code:bg-muted prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-pre:bg-muted prose-pre:border prose-pre:border-border prose-blockquote:border-l-primary prose-blockquote:text-muted-foreground prose-ul:text-foreground prose-ol:text-foreground prose-li:text-foreground prose-table:text-foreground prose-th:border-border prose-th:bg-muted prose-td:border-border max-w-none"
        >
          {note.content}
        </ReactMarkdown>
      </div>
    </div>
  );
}
