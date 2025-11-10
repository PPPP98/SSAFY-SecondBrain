import { useRef, useCallback, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { debounce } from 'lodash-es';
import {
  saveDraft,
  getDraft,
  deleteDraft as deleteDraftApi,
  saveToDatabase as saveToDatabaseApi,
  draftQueries,
} from '@/api/client/draftApi';
import type { NoteDraftRequest, NoteDraftResponse } from '@/shared/types/draft.types';

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

  // TanStack Query로 Draft 관리 (단일 진실 공급원)
  // placeholderData: 새 Draft의 경우 빈 값으로 시작
  const { data: draft, isLoading } = useQuery({
    queryKey: draftQueries.detail(draftId),
    queryFn: () => getDraft(draftId),
    placeholderData: {
      noteId: draftId,
      title: '',
      content: '',
      version: 1,
      lastModified: new Date().toISOString(),
    },
    retry: false,
    staleTime: Infinity,
    throwOnError: false, // 404 에러 무시 (새 Draft)
  });

  // TanStack Query 캐시에서 직접 파생 (이중 상태 관리 제거)
  const title = draft?.title ?? '';
  const content = draft?.content ?? '';
  const version = draft?.version ?? 1;

  // 자동 저장용 Refs
  const changeCountRef = useRef(0);
  const lastDbSaveTimeRef = useRef(Date.now());

  // beforeunload를 위한 최신 값 추적 (draft는 비동기이므로 ref 필요)
  const titleRef = useRef(title);
  const contentRef = useRef(content);

  // titleRef, contentRef를 draft 변경 시 동기화
  useEffect(() => {
    titleRef.current = title;
    contentRef.current = content;
  }, [title, content]);

  // Redis 저장 Mutation (Optimistic Update 적용)
  const saveMutation = useMutation({
    mutationFn: (data: NoteDraftRequest) => saveDraft(data),
    onMutate: async (newDraft) => {
      // Optimistic update: 즉시 캐시 업데이트
      await queryClient.cancelQueries({ queryKey: draftQueries.detail(draftId) });
      const previousDraft = queryClient.getQueryData(draftQueries.detail(draftId));

      queryClient.setQueryData(draftQueries.detail(draftId), {
        ...newDraft,
        lastModified: new Date().toISOString(),
      });

      return { previousDraft };
    },
    onSuccess: (response) => {
      // 서버 응답으로 캐시 업데이트
      queryClient.setQueryData(draftQueries.detail(draftId), response);
      changeCountRef.current++;

      // Batching 조건 체크
      void checkAndSaveToDatabase();
    },
    onError: (_error, _variables, context) => {
      // Rollback optimistic update
      if (context?.previousDraft) {
        queryClient.setQueryData(draftQueries.detail(draftId), context.previousDraft);
      }

      // Fallback: LocalStorage
      localStorage.setItem(
        `draft:${draftId}`,
        JSON.stringify({
          noteId: draftId,
          title: titleRef.current,
          content: contentRef.current,
          version,
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
  }, [draftId]);

  // 핸들러
  // TanStack Query Optimistic Update 패턴
  // 즉시 캐시 업데이트 → UI 반영 → 서버 저장
  function handleTitleChange(newTitle: string) {
    // Optimistic update: 즉시 캐시 업데이트
    queryClient.setQueryData<NoteDraftResponse>(
      draftQueries.detail(draftId),
      (old: NoteDraftResponse | undefined) =>
        old
          ? { ...old, title: newTitle }
          : {
              noteId: draftId,
              title: newTitle,
              content: contentRef.current,
              version,
              lastModified: new Date().toISOString(),
            },
    );

    // Debounced 서버 저장
    saveDraftToRedis({
      noteId: draftId,
      title: newTitle,
      content: contentRef.current,
      version,
    });
  }

  // ⚠️ Milkdown useEditor의 의존성 제약으로 useCallback 필요
  // NoteEditor의 useEditor([onChange])가 onChange 변경 시 에디터 재초기화
  const handleContentChange = useCallback(
    (newContent: string) => {
      // Optimistic update: 즉시 캐시 업데이트
      queryClient.setQueryData<NoteDraftResponse>(
        draftQueries.detail(draftId),
        (old: NoteDraftResponse | undefined) =>
          old
            ? { ...old, content: newContent }
            : {
                noteId: draftId,
                title: titleRef.current,
                content: newContent,
                version,
                lastModified: new Date().toISOString(),
              },
      );

      // Debounced 서버 저장
      saveDraftToRedis({
        noteId: draftId,
        title: titleRef.current,
        content: newContent,
        version,
      });
    },
    [draftId, version, saveDraftToRedis, queryClient],
  );

  const deleteDraft = async () => {
    await deleteDraftApi(draftId);
    queryClient.removeQueries({ queryKey: draftQueries.detail(draftId) });
  };

  return {
    title,
    content,
    version,
    lastModified: draft?.lastModified ? new Date(draft.lastModified) : null,
    isLoading,
    isSaving: saveMutation.isPending,
    handleTitleChange,
    handleContentChange,
    saveToDatabase,
    deleteDraft,
  };
}
