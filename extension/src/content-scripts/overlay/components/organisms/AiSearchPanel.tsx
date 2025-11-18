import { Loader2, Sparkles, FileText } from 'lucide-react';
import { NoteListItem } from '@/content-scripts/overlay/components/molecules/NoteListItem';
import {
  AiAnswerSkeleton,
  NoteListSkeleton,
} from '@/content-scripts/overlay/components/atoms/Skeleton';
import type { NoteSearchResult } from '@/types/note';

/**
 * AiSearchPanel (Organism)
 * - AI 검색 결과 패널
 * - AI 답변 (3줄) + Elasticsearch 노트 목록 (5개)
 * - 로딩/에러 상태 처리
 * - [전체보기] → Side Panel 열기
 */
interface AiSearchPanelProps {
  keyword: string;
  aiResponse: string;
  notesList: NoteSearchResult[];
  isLoading: boolean;
  esCompleted: boolean;
  error: string | null;
  onViewDetail: (noteId: number) => void;
}

export function AiSearchPanel({
  keyword,
  aiResponse,
  notesList,
  isLoading,
  esCompleted,
  error,
  onViewDetail,
}: AiSearchPanelProps) {
  // 에러 상태 (검색 시작 전)
  if (error && !isLoading && !aiResponse && notesList.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <div className="mb-2 rounded-full bg-red-50 p-2 dark:bg-red-950">
          <FileText className="h-6 w-6 text-red-500" />
        </div>
        <p className="text-sm font-medium text-foreground">검색 중 오류가 발생했습니다</p>
        <p className="mt-1 text-xs text-muted-foreground">{error}</p>
      </div>
    );
  }

  // 로딩 중이면서 아무 결과도 없을 때만 로딩 스피너 (ES도 완료 안됨)
  if (isLoading && !esCompleted && !aiResponse && notesList.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
          <p className="text-xs text-muted-foreground">검색 중...</p>
        </div>
      </div>
    );
  }

  // 모든 검색 완료했지만 결과 없음 (ES와 AI 모두 완료)
  if (!isLoading && !aiResponse && notesList.length === 0 && esCompleted) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <div className="mb-2 rounded-full bg-muted p-2">
          <FileText className="h-8 w-8 text-muted-foreground" />
        </div>
        <p className="text-sm font-medium text-foreground">
          &quot;{keyword}&quot;와 관련된 노트가 없습니다
        </p>
        <p className="mt-1 text-xs text-muted-foreground">다른 키워드로 검색해보세요</p>
      </div>
    );
  }

  // 검색 결과 표시 (Progressive: 관련 노트를 먼저 표시)
  return (
    <div className="flex w-[400px] flex-col gap-4">
      {/* 관련 노트 섹션 - 우선 표시 (빠른 피드백) */}
      {notesList.length > 0 ? (
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <FileText className="h-4 w-4" />
              관련 노트
            </h3>
          </div>

          {/* 노트 리스트에만 스크롤 적용 - 최대 5개 노트 표시, 약 400px */}
          <div className="flex max-h-[400px] flex-col gap-2 overflow-y-auto [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-muted [&::-webkit-scrollbar-thumb]:hover:bg-muted-foreground/50 [&::-webkit-scrollbar-track]:bg-transparent">
            {notesList.map((note) => (
              <NoteListItem
                key={note.id}
                note={note}
                similarity={note.score}
                onViewDetail={() => onViewDetail(note.id)}
              />
            ))}
          </div>
        </div>
      ) : esCompleted ? (
        <div className="rounded-lg border border-border bg-card p-4">
          <div className="mb-2 flex items-center gap-2">
            <FileText className="h-4 w-4 text-muted-foreground" />
            <h3 className="text-sm font-semibold text-foreground">관련 노트</h3>
          </div>
          <p className="text-sm text-muted-foreground">검색 결과가 없습니다</p>
        </div>
      ) : isLoading ? (
        <NoteListSkeleton count={5} />
      ) : null}

      {/* AI 답변 섹션 - 보조 정보로 나중에 표시 */}
      {aiResponse ? (
        <div className="rounded-lg border border-border bg-card p-4">
          <div className="mb-2 flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">AI 답변</h3>
          </div>
          <div className="max-h-[200px] overflow-y-auto [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-muted [&::-webkit-scrollbar-thumb]:hover:bg-muted-foreground/50 [&::-webkit-scrollbar-track]:bg-transparent">
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground">
              {aiResponse}
            </p>
          </div>
        </div>
      ) : isLoading ? (
        <AiAnswerSkeleton />
      ) : null}
    </div>
  );
}
