// 유사 노트 검색
export interface Note {
  id: number;
  title: string;
  content: string;
  userId: number;
  createdAt: string;
  updatedAt: string;
  remindCount: number;
}

export interface SimilarNoteRequest {
  noteId: number;
  limit: number;
}

export interface SimilarNoteResponse {
  success: boolean;
  code: number;
  message: string;
  data: Note[];
}

// 키워드 노트 검색
export interface SearchNoteRequest {
  keyword: string;
  page: number;
  size: number;
}

export interface SearchNoteData {
  results: Note[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
  pageSize: number;
}

export interface SearchNoteResponse {
  success: boolean;
  code: number;
  message: string;
  data: SearchNoteData;
}

// 최근 작성 노트
export interface RecentNote {
  noteId: number;
  title: string;
}

export interface RecentNoteResponse {
  success: boolean;
  code: number;
  message: string;
  data: RecentNote[];
}
