// 노트 조회
export interface NoteData {
  noteId: number;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  remindAt: string | null;
  remindCount: number;
}

export interface NoteGetRequest {
  noteId: number;
}

export interface NoteGetResponse {
  success: boolean;
  code: number;
  message: string;
  data: NoteData;
}
