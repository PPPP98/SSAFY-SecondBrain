/**
 * AI 검색 관련 타입 정의
 */

/**
 * AI Agent 검색 응답의 Document Schema
 */
export interface DocumentSchema {
  note_id: number;
  title: string;
  created_at?: string;
  updated_at?: string;
  similarity_score?: number;
}

/**
 * AI Agent 검색 API 응답
 */
export interface SearchResponse {
  success: boolean;
  response: string; // LLM이 생성한 자연어 답변
  documents: DocumentSchema[];
}

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
 * AI 검색 Store 상태
 */
export interface AiSearchState {
  // 검색 상태
  keyword: string;
  isSearching: boolean;
  isFocused: boolean;

  // AI 검색 결과
  aiResponse: string;
  aiDocuments: DocumentSchema[];

  // Elasticsearch 결과
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
