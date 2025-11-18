/**
 * 노트 검색 관련 타입 정의
 */

/**
 * 노트 상세 조회 응답
 */
export interface NoteDetail {
  noteId: number;
  title: string;
  content: string; // 마크다운 형식
  createdAt: string;
  updatedAt: string;
}

/**
 * 노트 검색 Store 상태
 */
export interface NoteSearchState {
  // 검색 상태
  keyword: string;
  isSearching: boolean;
  isFocused: boolean;

  // 검색 결과
  notesList: NoteSearchResult[];
  totalCount: number;

  // 에러
  error: string | null;

  // 액션
  setFocused: (focused: boolean) => void;
  search: (keyword: string) => Promise<void>;
  clearSearch: () => void;
  setKeyword: (keyword: string) => void;
}

/**
 * NoteSearchResult 임포트 (기존 타입 재사용)
 */
import type { NoteSearchResult } from './note';
