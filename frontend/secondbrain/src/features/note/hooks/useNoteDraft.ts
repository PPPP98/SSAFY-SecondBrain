import { useState, useRef, useCallback, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { debounce } from 'lodash-es';
import {
  saveDraft,
  getDraft,
  deleteDraft as deleteDraftApi,
  saveToDatabase as saveToDatabaseApi,
  draftQueries,
} from '@/api/client/draftApi';
import type { NoteDraftRequest } from '@/shared/types/draft.types';

interface UseNoteDraftOptions {
  draftId: string;
  onSaveToDatabase?: (noteId: number) => void;
}

interface UseNoteDraftReturn {
  // 상태
  title: string;
  content: string;
  version: number;
  lastModified: Date | null;
  isLoading: boolean;
  isSaving: boolean;

  // 핸들러
  handleTitleChange: (value: string) => void;
  handleContentChange: (value: string) => void;
  saveToDatabase: () => Promise<void>;
  deleteDraft: () => Promise<void>;
}

/**
 * Draft 자동 저장 훅
 *
 * 자동 저장 트리거:
 * 1. Debouncing (500ms) → POST /api/drafts
 * 2. Batching (50회 변경 or 5분 경과) → POST /api/notes/from-draft/{id}
 * 3. beforeunload → navigator.sendBeacon()
 *
 * @param options - draftId, onSaveToDatabase 콜백
 * @returns Draft 상태 및 핸들러
 */
export function useNoteDraft(options: UseNoteDraftOptions): UseNoteDraftReturn {
  const { draftId, onSaveToDatabase } = options;
  const queryClient = useQueryClient();

  // UI 렌더링용 상태 (useState)
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  // 자동 저장용 Refs
  const versionRef = useRef(1);
  const changeCountRef = useRef(0);
  const lastDbSaveTimeRef = useRef(Date.now());

  // beforeunload를 위한 최신 값 추적 refs
  const titleRef = useRef(title);
  const contentRef = useRef(content);

  // 기존 Draft 조회 (새 Draft는 404 에러 무시)
  const { data: existingDraft, isLoading } = useQuery({
    queryKey: draftQueries.detail(draftId),
    queryFn: () => getDraft(draftId),
    retry: false,
    staleTime: Infinity,
    throwOnError: false, // 404 에러 무시
  });

  // React 공식 문서 패턴: "Adjust state on prop change during rendering"
  // draftId 변경 시 상태 동기화 (렌더링 중)
  const [prevDraftId, setPrevDraftId] = useState(draftId);

  if (draftId !== prevDraftId) {
    // 다른 Draft로 전환됨 → 상태 리셋
    setPrevDraftId(draftId);

    const newTitle = existingDraft?.title ?? '';
    const newContent = existingDraft?.content ?? '';

    setTitle(newTitle);
    setContent(newContent);

    // refs도 함께 업데이트
    titleRef.current = newTitle;
    contentRef.current = newContent;
    versionRef.current = existingDraft?.version ?? 1;
  }

  // Redis 저장 Mutation
  const saveMutation = useMutation({
    mutationFn: (data: NoteDraftRequest) => saveDraft(data),
    onSuccess: (response) => {
      versionRef.current = response.version;
      changeCountRef.current++;
      queryClient.setQueryData(draftQueries.detail(draftId), response);

      // Batching 조건 체크
      void checkAndSaveToDatabase();
    },
    onError: () => {
      // Fallback: LocalStorage
      localStorage.setItem(
        `draft:${draftId}`,
        JSON.stringify({
          noteId: draftId,
          title,
          content,
          version: versionRef.current,
        }),
      );
    },
  });

  // Batching: DB 저장 조건 체크
  const checkAndSaveToDatabase = async () => {
    const timeSinceLastSave = Date.now() - lastDbSaveTimeRef.current;
    const shouldSave =
      changeCountRef.current >= 50 || // 50회 변경
      timeSinceLastSave >= 5 * 60 * 1000; // 5분 경과

    if (shouldSave) {
      await saveToDatabase();
    }
  };

  // DB 저장
  const saveToDatabase = async () => {
    // 최종 검증 (DB는 빈 값 불가)
    if (!title.trim() || !content.trim()) {
      return;
    }

    const noteId = await saveToDatabaseApi(draftId);

    // 카운터 초기화
    changeCountRef.current = 0;
    lastDbSaveTimeRef.current = Date.now();

    // Draft 삭제
    await deleteDraftApi(draftId);
    queryClient.removeQueries({ queryKey: draftQueries.detail(draftId) });

    // 콜백 호출
    if (onSaveToDatabase) {
      onSaveToDatabase(noteId);
    }
  };

  // Debounced Redis 저장 (500ms)
  // React 공식 패턴: debounce 함수는 useRef로 저장
  // saveMutation.mutate는 TanStack Query가 안정적인 참조 보장
  const saveDraftToRedis = useRef(
    debounce((draft: NoteDraftRequest) => {
      // 최소 검증: title 또는 content 중 하나라도 있어야 함
      if (!draft.title?.trim() && !draft.content?.trim()) {
        return;
      }

      saveMutation.mutate(draft);
    }, 500),
  ).current;

  // beforeunload: 페이지 이탈 시 자동 저장
  useEffect(() => {
    const handleBeforeUnload = () => {
      // ref로 최신 값 참조 (클로저 회피)
      if (titleRef.current.trim() && contentRef.current.trim()) {
        // Navigator.sendBeacon 사용 (비동기, 보장됨)
        navigator.sendBeacon(
          `/api/notes/from-draft/${draftId}`,
          new Blob(
            [
              JSON.stringify({
                title: titleRef.current,
                content: contentRef.current,
              }),
            ],
            {
              type: 'application/json',
            },
          ),
        );
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [draftId]); // title, content 제거: 타이핑마다 재실행 방지

  // 핸들러
  // React 공식: 자식이 memo()가 아니므로 useCallback 불필요
  // contentRef 사용으로 cross-dependency 제거
  function handleTitleChange(newTitle: string) {
    setTitle(newTitle);
    titleRef.current = newTitle;

    saveDraftToRedis({
      noteId: draftId,
      title: newTitle,
      content: contentRef.current, // ref로 최신 값 참조
      version: versionRef.current,
    });
  }

  // titleRef 사용으로 cross-dependency 제거
  // ⚠️ Milkdown useEditor의 의존성 제약으로 useCallback 필요
  // NoteEditor의 useEditor([onChange])가 onChange 변경 시 에디터 재초기화
  // 안정적인 참조가 없으면 타이핑 중 에디터가 계속 재생성되어 editorView 손실
  const handleContentChange = useCallback(
    (newContent: string) => {
      setContent(newContent);
      contentRef.current = newContent;

      saveDraftToRedis({
        noteId: draftId,
        title: titleRef.current, // ref로 최신 값 참조
        content: newContent,
        version: versionRef.current,
      });
    },
    [draftId, saveDraftToRedis],
  );

  const deleteDraft = async () => {
    await deleteDraftApi(draftId);
    queryClient.removeQueries({ queryKey: draftQueries.detail(draftId) });
  };

  return {
    title,
    content,
    version: versionRef.current,
    lastModified: existingDraft?.lastModified ? new Date(existingDraft.lastModified) : null,
    isLoading,
    isSaving: saveMutation.isPending,
    handleTitleChange,
    handleContentChange,
    saveToDatabase,
    deleteDraft,
  };
}
