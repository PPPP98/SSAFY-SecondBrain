// 유사 노트 검색
export interface SimilarNote {
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
  data: SimilarNote[];
}
