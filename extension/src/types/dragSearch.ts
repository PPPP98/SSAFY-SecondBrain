import type { NoteSearchResult } from './note';

// 드래그 검색 메시지 타입
export interface DragSearchMessage {
  type: 'SEARCH_DRAG_TEXT';
  keyword: string;
  timestamp: number;
}

export interface DragSearchResponse {
  type: 'DRAG_SEARCH_RESULT' | 'DRAG_SEARCH_ERROR';
  keyword: string;
  results?: NoteSearchResult[];
  totalCount?: number;
  error?: string;
}

// 플로팅 버튼 위치
export interface FloatingButtonPosition {
  x: number;
  y: number;
}

// 드래그 검색 설정
export interface DragSearchSettings {
  enabled: boolean;
  minTextLength: number;
  debounceMs: number;
  autoHideMs: number;
  excludedDomains: string[];
}

// 검색 히스토리 아이템
export interface SearchHistoryItem {
  keyword: string;
  timestamp: number;
  resultCount: number;
}
